package com.jaldishop.backend.identity.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findWithRolesByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findWithRolesById(UUID id);

    @Modifying
    @Query(value = "UPDATE user_roles SET store_id = :storeId WHERE user_id = :userId AND role_id = (SELECT id FROM roles WHERE name = 'MERCHANT')", nativeQuery = true)
    int updateStoreIdForMerchantRole(@Param("userId") UUID userId, @Param("storeId") UUID storeId);

    @Modifying
    @Query(value = "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), :userId, (SELECT id FROM roles WHERE name = 'MERCHANT'), :storeId)", nativeQuery = true)
    void insertMerchantRoleWithStore(@Param("userId") UUID userId, @Param("storeId") UUID storeId);

    default void assignStoreToMerchantRole(UUID userId, UUID storeId) {
        int updated = updateStoreIdForMerchantRole(userId, storeId);
        if (updated == 0) {
            insertMerchantRoleWithStore(userId, storeId);
        }
    }

    @Modifying
    @Query(value = "DELETE FROM user_roles WHERE user_id = :userId AND role_id = (SELECT id FROM roles WHERE name = 'MERCHANT')", nativeQuery = true)
    void removeMerchantRole(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "UPDATE users SET credentials_version = credentials_version + 1, updated_at = NOW() WHERE id = :userId", nativeQuery = true)
    int incrementCredentialsVersion(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "SET LOCAL lock_timeout = '2s'", nativeQuery = true)
    void applyShortLockTimeout();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            SELECT (
                (SELECT COUNT(*) FROM stores WHERE owner_user_id = :userId) +
                (SELECT COUNT(*) FROM orders WHERE user_id = :userId) +
                (SELECT COUNT(*) FROM reviews WHERE user_id = :userId) +
                (SELECT COUNT(*) FROM capacity_reservations WHERE user_id = :userId) +
                (SELECT COUNT(*) FROM payments WHERE capacity_reservation_id IN (SELECT id FROM capacity_reservations WHERE user_id = :userId)) +
                (SELECT COUNT(*) FROM admin_audit_logs WHERE actor_id = :userId)
            ) > 0
            """, nativeQuery = true)
    boolean hasOperationalOrAuditActorHistory(@Param("userId") UUID userId);

    @Modifying
    @Query(value = """
            DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = :userId);
            DELETE FROM carts WHERE user_id = :userId;
            DELETE FROM favorites WHERE user_id = :userId;
            DELETE FROM customer_addresses WHERE user_id = :userId;
            DELETE FROM notifications WHERE user_id = :userId;
            DELETE FROM password_reset_tokens WHERE user_id = :userId;
            DELETE FROM user_roles WHERE user_id = :userId;
            DELETE FROM users WHERE id = :userId;
            """, nativeQuery = true)
    void deleteUserAndSafeOrphans(@Param("userId") UUID userId);

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN u.roles r WHERE " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:role IS NULL OR r.name = :role) AND " +
            "(:query IS NULL OR :query = '' OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY u.createdAt DESC")
    List<UserEntity> searchUsers(
            @Param("query") String query,
            @Param("role") RoleName role,
            @Param("status") UserStatus status
    );
}
