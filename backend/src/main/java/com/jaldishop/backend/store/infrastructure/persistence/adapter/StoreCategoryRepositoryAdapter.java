package com.jaldishop.backend.store.infrastructure.persistence.adapter;

import com.jaldishop.backend.store.domain.StoreCategory;
import com.jaldishop.backend.store.domain.StoreCategoryRepository;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCategoryEntity;
import com.jaldishop.backend.store.infrastructure.persistence.mapper.StoreCategoryPersistenceMapper;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreCategoryJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StoreCategoryRepositoryAdapter implements StoreCategoryRepository {

    private final StoreCategoryJpaRepository jpaRepository;
    private final StoreCategoryPersistenceMapper mapper;

    public StoreCategoryRepositoryAdapter(StoreCategoryJpaRepository jpaRepository, StoreCategoryPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StoreCategory save(StoreCategory storeCategory) {
        StoreCategoryEntity entity = mapper.toEntity(storeCategory);
        StoreCategoryEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<StoreCategory> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<StoreCategory> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug).map(mapper::toDomain);
    }

    @Override
    public List<StoreCategory> findAll() {
        return jpaRepository.findAllByStatusOrderByNameAsc("ACTIVE").stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }
}
