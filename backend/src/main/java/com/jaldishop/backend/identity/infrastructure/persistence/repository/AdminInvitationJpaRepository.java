package com.jaldishop.backend.identity.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.AdminInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AdminInvitationJpaRepository extends JpaRepository<AdminInvitationEntity, UUID> {

    Optional<AdminInvitationEntity> findByTokenHash(String tokenHash);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AdminInvitationEntity a WHERE a.tokenHash = :tokenHash")
    Optional<AdminInvitationEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    org.springframework.data.domain.Page<AdminInvitationEntity> findByStatus(
            AdminInvitationStatus status,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT a FROM AdminInvitationEntity a WHERE LOWER(a.email) = LOWER(:email) AND a.status = :status")
    Optional<AdminInvitationEntity> findByEmailAndStatus(
            @Param("email") String email,
            @Param("status") AdminInvitationStatus status
    );

    @Query("SELECT COUNT(a) > 0 FROM AdminInvitationEntity a WHERE LOWER(a.email) = LOWER(:email) AND a.status = 'PENDING'")
    boolean existsPendingByEmail(@Param("email") String email);
}
