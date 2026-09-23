package com.jaldishop.backend.catalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(UUID id);
    Optional<Category> findByIdAndStoreId(UUID id, UUID storeId);
    List<Category> findByStoreId(UUID storeId);
    boolean existsByStoreIdAndNameIgnoreCase(UUID storeId, String name);
    boolean existsByStoreIdAndNameIgnoreCaseAndIdNot(UUID storeId, String name, UUID id);
}