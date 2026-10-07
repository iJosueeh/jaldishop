package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.jaldishop.backend.store.infrastructure.persistence.projection.StoreCustomerProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StoreCustomerJpaRepository extends JpaRepository<UserEntity, UUID> {

    @Query(value = "SELECT COUNT(*) > 0 FROM user_roles WHERE store_id = :storeId AND user_id = :userId AND role_id = 1", nativeQuery = true)
    boolean existsByStoreIdAndUserId(@Param("storeId") UUID storeId, @Param("userId") UUID userId);

    @Modifying
    @Query(value = "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), :userId, 1, :storeId) ON CONFLICT (user_id, role_id, store_id) WHERE store_id IS NOT NULL DO NOTHING", nativeQuery = true)
    void assignCustomerToStore(@Param("storeId") UUID storeId, @Param("userId") UUID userId);

    @Query(value = """
            SELECT u.id AS userId,
                   u.first_name AS firstName,
                   u.last_name AS lastName,
                   u.email AS email,
                   u.phone AS phone,
                   u.created_at AS customerSince,
                   COUNT(o.id) AS ordersCount,
                   COALESCE(SUM(CASE WHEN o.status != 'CANCELLED' THEN o.total_amount ELSE 0 END), 0) AS totalSpent,
                   MAX(o.confirmed_at) AS lastOrderAt
            FROM user_roles ur
            JOIN users u ON u.id = ur.user_id
            LEFT JOIN orders o ON o.user_id = ur.user_id AND o.store_id = ur.store_id
            WHERE ur.store_id = :storeId AND ur.role_id = 1
              AND (:query IS NULL OR :query = '' OR
                   LOWER(u.first_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(u.last_name) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) OR
                   (u.phone IS NOT NULL AND u.phone LIKE CONCAT('%', :query, '%')))
            GROUP BY u.id, u.first_name, u.last_name, u.email, u.phone, u.created_at
            ORDER BY u.created_at DESC
            """, nativeQuery = true)
    List<StoreCustomerProjection> findCustomersByStoreId(
            @Param("storeId") UUID storeId,
            @Param("query") String query
    );
}
