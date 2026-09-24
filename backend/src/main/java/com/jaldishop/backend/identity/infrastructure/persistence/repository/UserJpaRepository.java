package com.jaldishop.backend.identity.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
