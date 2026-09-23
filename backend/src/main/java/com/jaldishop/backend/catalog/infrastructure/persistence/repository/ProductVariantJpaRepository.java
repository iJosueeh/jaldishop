package com.jaldishop.backend.catalog.infrastructure.persistence.repository;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantEntity, UUID> {
    List<ProductVariantEntity> findByProductId(UUID productId);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
}