package com.jaldishop.backend.store.domain;

import java.util.Optional;
import java.util.UUID;

public interface StoreRepository {
    Store save(Store store);
    Optional<Store> findById(UUID id);
    Optional<Store> findByMerchantUserId(UUID merchantUserId);
    Optional<Store> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByMerchantUserId(UUID merchantUserId);
}
