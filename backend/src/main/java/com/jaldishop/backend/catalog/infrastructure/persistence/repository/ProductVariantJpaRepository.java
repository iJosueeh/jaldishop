package com.jaldishop.backend.catalog.infrastructure.persistence.repository;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantEntity, UUID> {
    List<ProductVariantEntity> findByProductId(UUID productId);
    List<ProductVariantEntity> findByStoreId(UUID storeId);
    Optional<ProductVariantEntity> findByIdAndStoreId(UUID id, UUID storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariantEntity pv WHERE pv.id = :id AND pv.storeId = :storeId")
    Optional<ProductVariantEntity> findByIdAndStoreIdForUpdate(@Param("id") UUID id, @Param("storeId") UUID storeId);

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, UUID id);
    boolean existsByStoreIdAndSku(UUID storeId, String sku);
    boolean existsByStoreIdAndSkuAndIdNot(UUID storeId, String sku, UUID id);
}
