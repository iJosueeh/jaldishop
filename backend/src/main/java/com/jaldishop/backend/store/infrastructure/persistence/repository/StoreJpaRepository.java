package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreJpaRepository extends JpaRepository<StoreEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "categories")
    Optional<StoreEntity> findById(UUID id);

    @EntityGraph(attributePaths = "categories")
    Optional<StoreEntity> findByOwnerUserId(UUID ownerUserId);

    default Optional<StoreEntity> findByMerchantUserId(UUID merchantUserId) {
        return findByOwnerUserId(merchantUserId);
    }

    @EntityGraph(attributePaths = "categories")
    Optional<StoreEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
    boolean existsByOwnerUserId(UUID ownerUserId);

    default boolean existsByMerchantUserId(UUID merchantUserId) {
        return existsByOwnerUserId(merchantUserId);
    }

    @EntityGraph(attributePaths = "categories")
    @Query("SELECT s FROM StoreEntity s WHERE " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.slug) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY s.createdAt DESC")
    List<StoreEntity> searchStores(
            @Param("query") String query,
            @Param("status") StoreStatus status
    );

    @Query(value = """
            SELECT (
                (SELECT COUNT(*) FROM orders WHERE store_id = :storeId) +
                (SELECT COUNT(*) FROM capacity_reservations WHERE store_id = :storeId)
            ) > 0
            """, nativeQuery = true)
    boolean hasOperationalHistory(@Param("storeId") UUID storeId);

    @Modifying
    @Query(value = """
            DELETE FROM cart_items WHERE store_id = :storeId;
            DELETE FROM carts WHERE store_id = :storeId;
            DELETE FROM discounts WHERE store_id = :storeId;
            DELETE FROM product_images WHERE product_id IN (SELECT id FROM products WHERE store_id = :storeId);
            DELETE FROM variant_attributes WHERE variant_id IN (SELECT id FROM product_variants WHERE store_id = :storeId);
            DELETE FROM inventories WHERE variant_id IN (SELECT id FROM product_variants WHERE store_id = :storeId);
            DELETE FROM product_variants WHERE store_id = :storeId;
            DELETE FROM products WHERE store_id = :storeId;
            DELETE FROM categories WHERE store_id = :storeId;
            DELETE FROM capacity_configurations WHERE store_id = :storeId;
            DELETE FROM capacity_exceptions WHERE store_id = :storeId;
            DELETE FROM store_category_assignments WHERE store_id = :storeId;
            DELETE FROM user_roles WHERE store_id = :storeId;
            DELETE FROM stores WHERE id = :storeId;
            """, nativeQuery = true)
    void deleteStoreAndDraftCatalog(@Param("storeId") UUID storeId);
}
