package com.jaldishop.backend.store.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository {
    Store save(Store store);
    Optional<Store> findById(UUID id);
    Optional<Store> findByOwnerUserId(UUID ownerUserId);
    default Optional<Store> findByMerchantUserId(UUID merchantUserId) {
        return findByOwnerUserId(merchantUserId);
    }
    Optional<Store> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByOwnerUserId(UUID ownerUserId);
    default boolean existsByMerchantUserId(UUID merchantUserId) {
        return existsByOwnerUserId(merchantUserId);
    }
    List<Store> findAllStores(String query, StoreStatus status);
    boolean hasOperationalHistory(UUID storeId);
    void deleteStore(UUID storeId, UUID ownerUserId);
}
