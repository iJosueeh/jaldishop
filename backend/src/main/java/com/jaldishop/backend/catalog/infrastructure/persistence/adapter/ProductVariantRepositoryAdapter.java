package com.jaldishop.backend.catalog.infrastructure.persistence.adapter;

import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.infrastructure.persistence.mapper.ProductVariantPersistenceMapper;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductVariantJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductVariantRepositoryAdapter implements ProductVariantRepository {

    private final ProductVariantJpaRepository repository;
    private final ProductVariantPersistenceMapper mapper;

    public ProductVariantRepositoryAdapter(ProductVariantJpaRepository repository, ProductVariantPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ProductVariant save(ProductVariant variant) {
        var entity = mapper.toEntity(variant);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ProductVariant> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProductVariant> findByProductId(UUID productId) {
        return repository.findByProductId(productId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySku(String sku) {
        return repository.existsBySku(sku);
    }

    @Override
    public boolean existsBySkuAndIdNot(String sku, UUID id) {
        return repository.existsBySkuAndIdNot(sku, id);
    }
}