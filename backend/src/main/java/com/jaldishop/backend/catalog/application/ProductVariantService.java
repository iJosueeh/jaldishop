package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o no pertenece a la tienda"));

        if (command.presentationName() == null || command.presentationName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la presentación no puede estar vacío");
        }

        if (command.priceAmount() == null || command.priceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }

        // Regla de unicidad de SKU
        if (command.sku() != null && !command.sku().isBlank()) {
            String trimmedSku = command.sku().trim();
            if (variantRepository.existsBySku(trimmedSku)) {
                throw new ConflictException("SKU_ALREADY_EXISTS", "El SKU '" + trimmedSku + "' ya está en uso");
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
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o no pertenece a la tienda"));

        return variantRepository.findByProductId(productId);
    }

    @Transactional(readOnly = true)
    public ProductVariant getVariantById(UUID variantId, UUID productId, UUID storeId) {
        productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o no pertenece a la tienda"));

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));

        if (!variant.getProductId().equals(productId)) {
            throw new IllegalArgumentException("La variante no pertenece al producto especificado");
        }

        return variant;
    }

    @Transactional
    public ProductVariant updateVariant(UpdateProductVariantCommand command) {
        ProductVariant variant = getVariantById(command.variantId(), command.productId(), command.storeId());

        if (command.presentationName() == null || command.presentationName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la presentación no puede estar vacío");
        }

        // Regla de unicidad de SKU
        if (command.sku() != null && !command.sku().isBlank()) {
            String trimmedSku = command.sku().trim();
            if (variantRepository.existsBySkuAndIdNot(trimmedSku, command.variantId())) {
                throw new ConflictException("SKU_ALREADY_EXISTS", "El SKU '" + trimmedSku + "' ya está en uso");
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