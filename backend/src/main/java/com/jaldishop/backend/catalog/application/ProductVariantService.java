package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public ProductVariantService(ProductVariantRepository variantRepository, ProductRepository productRepository) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductVariant createVariant(CreateProductVariantCommand command) {
        // Validar pertenencia del producto a la tienda
        productRepository.findByIdAndStoreId(command.productId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found or does not belong to store"));

        if (command.presentationName() == null || command.presentationName().trim().isEmpty()) {
            throw new IllegalArgumentException("Presentation name cannot be empty");
        }

        if (command.priceAmount() == null || command.priceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price amount must be greater than zero");
        }

        // Regla de unicidad de SKU
        if (command.sku() != null && !command.sku().isBlank()) {
            String trimmedSku = command.sku().trim();
            if (variantRepository.existsBySku(trimmedSku)) {
                throw new ConflictException("SKU '" + trimmedSku + "' is already in use");
            }
        }

        ProductVariant variant = ProductVariant.create(
                command.productId(),
                command.presentationName().trim(),
                command.sku(),
                command.priceAmount(),
                command.priceCurrency(),
                command.tracksInventory(),
                command.attributes()
        );

        return variantRepository.save(variant);
    }

    @Transactional(readOnly = true)
    public List<ProductVariant> getVariantsByProduct(UUID productId, UUID storeId) {
        // Validar que el producto pertenezca a la tienda
        productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or does not belong to store"));

        return variantRepository.findByProductId(productId);
    }

    @Transactional(readOnly = true)
    public ProductVariant getVariantById(UUID variantId, UUID productId, UUID storeId) {
        productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or does not belong to store"));

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new IllegalArgumentException("Variant not found"));

        if (!variant.getProductId().equals(productId)) {
            throw new IllegalArgumentException("Variant does not belong to the given product");
        }

        return variant;
    }

    @Transactional
    public ProductVariant updateVariant(UpdateProductVariantCommand command) {
        ProductVariant variant = getVariantById(command.variantId(), command.productId(), command.storeId());

        if (command.presentationName() == null || command.presentationName().trim().isEmpty()) {
            throw new IllegalArgumentException("Presentation name cannot be empty");
        }

        // Regla de unicidad de SKU
        if (command.sku() != null && !command.sku().isBlank()) {
            String trimmedSku = command.sku().trim();
            if (variantRepository.existsBySkuAndIdNot(trimmedSku, command.variantId())) {
                throw new ConflictException("SKU '" + trimmedSku + "' is already in use");
            }
        }

        variant.update(
                command.presentationName().trim(),
                command.sku(),
                command.priceAmount(),
                command.priceCurrency(),
                command.tracksInventory(),
                command.attributes()
        );

        if (command.status() != null && !command.status().isBlank()) {
            VariantStatus newStatus = VariantStatus.valueOf(command.status().trim().toUpperCase());
            if (newStatus == VariantStatus.ACTIVE) {
                variant.activate();
            } else {
                variant.deactivate();
            }
        }

        return variantRepository.save(variant);
    }
}