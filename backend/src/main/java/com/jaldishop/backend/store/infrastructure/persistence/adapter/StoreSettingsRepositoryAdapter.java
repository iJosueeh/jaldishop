package com.jaldishop.backend.store.infrastructure.persistence.adapter;

import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.domain.repository.StoreSettingsRepository;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreSettingsEntity;
import com.jaldishop.backend.store.infrastructure.persistence.mapper.StoreSettingsPersistenceMapper;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreJpaRepository;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreSettingsJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class StoreSettingsRepositoryAdapter implements StoreSettingsRepository {

    private final StoreSettingsJpaRepository jpaRepository;
    private final StoreJpaRepository storeJpaRepository;
    private final StoreSettingsPersistenceMapper mapper;

    public StoreSettingsRepositoryAdapter(
            StoreSettingsJpaRepository jpaRepository,
            StoreJpaRepository storeJpaRepository,
            StoreSettingsPersistenceMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.storeJpaRepository = storeJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<StoreSettings> findByStoreId(UUID storeId) {
        return jpaRepository.findById(storeId).map(mapper::toDomain);
    }

    @Override
    public StoreSettings save(StoreSettings settings) {
        StoreSettingsEntity entity = mapper.toEntity(settings);
        if (entity.getStore() == null) {
            StoreEntity storeEntity = storeJpaRepository.getReferenceById(settings.getStoreId());
            entity.setStore(storeEntity);
        }
        StoreSettingsEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}
