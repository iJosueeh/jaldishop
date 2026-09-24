package com.jaldishop.backend.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    Optional<Product> findByIdAndStoreId(UUID id, UUID storeId);
    List<Product> findByStoreId(UUID storeId);
    List<Product> findByStoreIdAndCategoryId(UUID storeId, UUID categoryId);
    boolean existsByStoreIdAndSlug(UUID storeId, String slug);
    boolean existsByStoreIdAndSlugAndIdNot(UUID storeId, String slug, UUID id);
}