package com.jaldishop.backend.catalog.infrastructure.persistence.repository;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Modifying
    @Query(value = "SET LOCAL lock_timeout = '2s'", nativeQuery = true)
    void applyShortLockTimeout();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id AND p.storeId = :storeId")
    Optional<ProductEntity> findByIdAndStoreIdForUpdate(@Param("id") UUID id, @Param("storeId") UUID storeId);

    @Query(value = """
            SELECT (
                (SELECT COUNT(*) FROM order_items oi
                 JOIN product_variants pv ON pv.id = oi.variant_id
                 WHERE pv.product_id = :productId) +
                (SELECT COUNT(*) FROM inventory_reservations ir
                 JOIN product_variants pv ON pv.id = ir.variant_id
                 WHERE pv.product_id = :productId) +
                (SELECT COUNT(*) FROM reviews WHERE product_id = :productId)
            ) > 0
            """, nativeQuery = true)
    boolean hasOperationalHistory(@Param("productId") UUID productId);

    @Query(value = """
            SELECT image_url FROM product_images WHERE product_id = :productId
            UNION
            SELECT image_url FROM products WHERE id = :productId AND image_url IS NOT NULL
            """, nativeQuery = true)
    List<String> findProductImageUrls(@Param("productId") UUID productId);

    @Modifying
    @Query(value = """
            DELETE FROM cart_items WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId);
            DELETE FROM favorites WHERE product_id = :productId;
            DELETE FROM product_images WHERE product_id = :productId;
            DELETE FROM variant_attributes WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId);
            DELETE FROM inventories WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id = :productId);
            DELETE FROM product_variants WHERE product_id = :productId;
            DELETE FROM products WHERE id = :productId AND store_id = :storeId;
            """, nativeQuery = true)
    void deleteProductAndSafeOrphans(@Param("productId") UUID productId, @Param("storeId") UUID storeId);
}
