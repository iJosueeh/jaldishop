package com.jaldishop.backend.catalog.infrastructure.persistence.repository;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {
    Optional<ProductEntity> findByIdAndStoreId(UUID id, UUID storeId);
    List<ProductEntity> findByStoreId(UUID storeId);
    List<ProductEntity> findByStoreIdAndCategoryId(UUID storeId, UUID categoryId);
    boolean existsByStoreIdAndSlug(UUID storeId, String slug);
    boolean existsByStoreIdAndSlugAndIdNot(UUID storeId, String slug, UUID id);
}