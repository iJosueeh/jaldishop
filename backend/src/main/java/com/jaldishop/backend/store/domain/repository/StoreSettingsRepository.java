package com.jaldishop.backend.store.domain.repository;

import com.jaldishop.backend.store.domain.model.StoreSettings;

import java.util.Optional;
import java.util.UUID;

public interface StoreSettingsRepository {
    Optional<StoreSettings> findByStoreId(UUID storeId);
    StoreSettings save(StoreSettings settings);
}
