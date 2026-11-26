package com.jaldishop.backend.catalog.infrastructure.persistence.adapter;

import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.catalog.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductJpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
    public Optional<Product> findByStoreIdAndSlug(UUID storeId, String slug) {
        return repository.findByStoreIdAndSlug(storeId, slug).map(mapper::toDomain);
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
    @Override
    public Optional<Product> lockByIdAndStoreIdForUpdate(UUID id, UUID storeId) {
        repository.applyShortLockTimeout();
        return repository.findByIdAndStoreIdForUpdate(id, storeId).map(mapper::toDomain);
    }

    @Override
    public boolean hasOperationalHistory(UUID productId) {
        return repository.hasOperationalHistory(productId);
    }

    @Override
    public List<String> findProductImageUrls(UUID productId) {
        return repository.findProductImageUrls(productId);
    }

    @Override
    public List<String> findOrderedImageUrls(UUID productId) {
        List<String> raw = repository.findOrderedProductImageUrls(productId);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String url : raw) {
            if (url != null && !url.isBlank()) {
                unique.add(url.trim());
            }
        }
        return unique.isEmpty() ? Collections.emptyList() : new ArrayList<>(unique);
    }

    @Override
    public Map<UUID, List<String>> findOrderedImageUrlsByProductIds(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = repository.findOrderedProductImageRowsByProductIds(productIds);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, LinkedHashSet<String>> grouped = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            UUID productId = row[0] instanceof UUID uuid ? uuid : UUID.fromString(row[0].toString());
            String url = row[1].toString().trim();
            if (!url.isEmpty()) {
                grouped.computeIfAbsent(productId, ignored -> new LinkedHashSet<>()).add(url);
            }
        }
        Map<UUID, List<String>> result = new LinkedHashMap<>();
        grouped.forEach((productId, urls) -> result.put(productId, new ArrayList<>(urls)));
        return result;
    }

    @Override
    public void deleteProductAndSafeOrphans(UUID productId, UUID storeId) {
        repository.deleteProductAndSafeOrphans(productId, storeId);
    }
}
