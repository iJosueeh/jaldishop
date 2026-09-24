package com.jaldishop.backend.catalog.infrastructure.persistence.adapter;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.infrastructure.persistence.mapper.CategoryPersistenceMapper;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.CategoryJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository repository;
    private final CategoryPersistenceMapper mapper;

    public CategoryRepositoryAdapter(CategoryJpaRepository repository, CategoryPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Category save(Category category) {
        var entity = mapper.toEntity(category);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Category> findByIdAndStoreId(UUID id, UUID storeId) {
        return repository.findByIdAndStoreId(id, storeId).map(mapper::toDomain);
    }

    @Override
    public List<Category> findByStoreId(UUID storeId) {
        return repository.findByStoreId(storeId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByStoreIdAndNameIgnoreCase(UUID storeId, String name) {
        return repository.existsByStoreIdAndNameIgnoreCase(storeId, name);
    }

    @Override
    public boolean existsByStoreIdAndNameIgnoreCaseAndIdNot(UUID storeId, String name, UUID id) {
        return repository.existsByStoreIdAndNameIgnoreCaseAndIdNot(storeId, name, id);
    }
}