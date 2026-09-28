package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class CreateProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public CreateProductVariantService(ProductVariantRepository variantRepository, ProductRepository productRepository) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    public ProductVariant execute(CreateProductVariantCommand command) {
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
}
