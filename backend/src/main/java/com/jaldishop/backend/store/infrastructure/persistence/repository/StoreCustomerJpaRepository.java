package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCustomerEntity;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCustomerId;
import com.jaldishop.backend.store.infrastructure.persistence.projection.StoreCustomerProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreCustomerJpaRepository extends JpaRepository<StoreCustomerEntity, StoreCustomerId> {

    boolean existsByIdStoreIdAndIdUserId(UUID storeId, UUID userId);

    @Query(value = """
            SELECT u.id AS userId,
                   u.first_name AS firstName,
                   u.last_name AS lastName,
                   u.email AS email,
                   u.phone AS phone,
                   sc.created_at AS customerSince,
                   COUNT(o.id) AS ordersCount,
                   COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.total_amount ELSE 0 END), 0) AS totalSpent,
                   MAX(o.confirmed_at) AS lastOrderAt
            FROM store_customers sc
            JOIN users u ON u.id = sc.user_id
            LEFT JOIN orders o ON o.user_id = sc.user_id AND o.store_id = sc.store_id
            WHERE sc.store_id = :storeId
              AND (:query IS NULL OR :query = '' OR
                   LOWER(u.first_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(u.last_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   (u.phone IS NOT NULL AND u.phone LIKE CONCAT('%', :query, '%')))
            GROUP BY u.id, u.first_name, u.last_name, u.email, u.phone, sc.created_at
            ORDER BY sc.created_at DESC
            """, nativeQuery = true)
    List<StoreCustomerProjection> findCustomersByStoreId(
            @Param("storeId") UUID storeId,
            @Param("query") String query
    );
}
