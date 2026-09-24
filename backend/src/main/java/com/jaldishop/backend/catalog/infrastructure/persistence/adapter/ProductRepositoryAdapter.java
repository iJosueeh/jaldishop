package com.jaldishop.backend.catalog.infrastructure.persistence.adapter;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository repository;
    private final ProductPersistenceMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository repository, ProductPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Product save(Product product) {
        var entity = mapper.toEntity(product);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findByIdAndStoreId(UUID id, UUID storeId) {
        return repository.findByIdAndStoreId(id, storeId).map(mapper::toDomain);
    }

    @Override
    public List<Product> findByStoreId(UUID storeId) {
        return repository.findByStoreId(storeId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByStoreIdAndCategoryId(UUID storeId, UUID categoryId) {
        return repository.findByStoreIdAndCategoryId(storeId, categoryId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByStoreIdAndSlug(UUID storeId, String slug) {
        return repository.existsByStoreIdAndSlug(storeId, slug);
    }

    @Override
    public boolean existsByStoreIdAndSlugAndIdNot(UUID storeId, String slug, UUID id) {
        return repository.existsByStoreIdAndSlugAndIdNot(storeId, slug, id);
    }
}