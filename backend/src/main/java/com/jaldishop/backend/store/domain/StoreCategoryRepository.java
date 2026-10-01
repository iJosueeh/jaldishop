package com.jaldishop.backend.store.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreCategoryRepository {
    StoreCategory save(StoreCategory storeCategory);
    Optional<StoreCategory> findById(UUID id);
    Optional<StoreCategory> findBySlug(String slug);
    List<StoreCategory> findAll();
    boolean existsBySlug(String slug);
}
