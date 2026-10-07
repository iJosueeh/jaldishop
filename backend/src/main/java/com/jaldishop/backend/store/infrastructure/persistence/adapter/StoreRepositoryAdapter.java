package com.jaldishop.backend.store.infrastructure.persistence.adapter;

import com.jaldishop.backend.identity.infrastructure.persistence.repository.UserJpaRepository;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCategoryEntity;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import com.jaldishop.backend.store.infrastructure.persistence.mapper.StorePersistenceMapper;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreCategoryJpaRepository;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StoreRepositoryAdapter implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;
    private final StoreCategoryJpaRepository storeCategoryJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final StorePersistenceMapper mapper;

    public StoreRepositoryAdapter(
            StoreJpaRepository storeJpaRepository,
            StoreCategoryJpaRepository storeCategoryJpaRepository,
            UserJpaRepository userJpaRepository,
            StorePersistenceMapper mapper
    ) {
        this.storeJpaRepository = storeJpaRepository;
        this.storeCategoryJpaRepository = storeCategoryJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Store save(Store store) {
        StoreEntity entity = mapper.toEntity(store);
        if (store.getCategoryIds() != null && !store.getCategoryIds().isEmpty()) {
            List<StoreCategoryEntity> categories =
                    storeCategoryJpaRepository.findAllById(store.getCategoryIds());
            entity.setCategories(new HashSet<>(categories));
        } else {
            entity.setCategories(new HashSet<>());
        }
        StoreEntity saved = storeJpaRepository.save(entity);
        userJpaRepository.assignStoreToMerchantRole(saved.getOwnerUserId(), saved.getId());
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Store> findById(UUID id) {
        return storeJpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Store> findByOwnerUserId(UUID ownerUserId) {
        return storeJpaRepository.findByOwnerUserId(ownerUserId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Store> findByMerchantUserId(UUID merchantUserId) {
        return findByOwnerUserId(merchantUserId);
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
    public boolean existsByOwnerUserId(UUID ownerUserId) {
        return storeJpaRepository.existsByOwnerUserId(ownerUserId);
    }

    @Override
    public boolean existsByMerchantUserId(UUID merchantUserId) {
        return existsByOwnerUserId(merchantUserId);
    }

    @Override
    public List<Store> findAllStores(String query, StoreStatus status) {
        String cleanQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return storeJpaRepository.searchStores(cleanQuery, status).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean hasOperationalHistory(UUID storeId) {
        return storeJpaRepository.hasOperationalHistory(storeId);
    }

    @Override
    public void deleteStore(UUID storeId, UUID ownerUserId) {
        storeJpaRepository.deleteStoreAndDraftCatalog(storeId);
        userJpaRepository.removeMerchantRole(ownerUserId);
    }
}
