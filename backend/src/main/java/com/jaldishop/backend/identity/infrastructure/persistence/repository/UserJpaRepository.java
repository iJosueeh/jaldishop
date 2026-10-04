package com.jaldishop.backend.identity.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
