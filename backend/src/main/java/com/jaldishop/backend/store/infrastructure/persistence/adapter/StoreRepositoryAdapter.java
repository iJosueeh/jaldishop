package com.jaldishop.backend.store.infrastructure.persistence.adapter;

import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import com.jaldishop.backend.store.infrastructure.persistence.mapper.StorePersistenceMapper;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class StoreRepositoryAdapter implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;
    private final StorePersistenceMapper mapper;

    public StoreRepositoryAdapter(StoreJpaRepository storeJpaRepository, StorePersistenceMapper mapper) {
        this.storeJpaRepository = storeJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Store save(Store store) {
        StoreEntity entity = mapper.toEntity(store);
        StoreEntity saved = storeJpaRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Store> findById(UUID id) {
        return storeJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Store> findByMerchantUserId(UUID merchantUserId) {
        return storeJpaRepository.findByMerchantUserId(merchantUserId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Store> findBySlug(String slug) {
        return storeJpaRepository.findBySlug(slug)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return storeJpaRepository.existsBySlug(slug);
    }

    @Override
    public boolean existsByMerchantUserId(UUID merchantUserId) {
        return storeJpaRepository.existsByMerchantUserId(merchantUserId);
    }

}
