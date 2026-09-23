package com.jaldishop.backend.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository {
    ProductVariant save(ProductVariant variant);
    Optional<ProductVariant> findById(UUID id);
    List<ProductVariant> findByProductId(UUID productId);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
}