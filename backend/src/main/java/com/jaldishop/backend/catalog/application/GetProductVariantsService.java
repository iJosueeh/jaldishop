package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProductVariantsService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public GetProductVariantsService(ProductVariantRepository variantRepository, ProductRepository productRepository) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    public List<ProductVariant> execute(UUID productId, UUID storeId) {
        productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o no pertenece a la tienda"));

        return variantRepository.findByProductId(productId);
    }

    public ProductVariant execute(UUID variantId, UUID productId, UUID storeId) {
        productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o no pertenece a la tienda"));

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variante no encontrada"));

        if (!variant.getProductId().equals(productId)) {
            throw new IllegalArgumentException("La variante no pertenece al producto especificado");
        }

        return variant;
    }
}
