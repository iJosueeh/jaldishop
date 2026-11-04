package com.jaldishop.backend.catalog.domain.repository;

import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository {
    ProductVariant save(ProductVariant variant);
    Optional<ProductVariant> findById(UUID id);
    Optional<ProductVariant> findByIdAndStoreId(UUID id, UUID storeId);
    List<ProductVariant> findByProductId(UUID productId);
    List<ProductVariant> findByStoreId(UUID storeId);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
    boolean existsByStoreIdAndSku(UUID storeId, String sku);
    boolean existsByStoreIdAndSkuAndIdNot(UUID storeId, String sku, UUID id);
}