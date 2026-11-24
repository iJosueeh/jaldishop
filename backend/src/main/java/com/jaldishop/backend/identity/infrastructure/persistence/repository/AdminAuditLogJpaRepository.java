package com.jaldishop.backend.identity.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.infrastructure.persistence.entity.AdminAuditLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminAuditLogJpaRepository extends JpaRepository<AdminAuditLogEntity, UUID>, JpaSpecificationExecutor<AdminAuditLogEntity> {
    List<AdminAuditLogEntity> findByActionInOrderByCreatedAtDesc(List<String> actions, Pageable pageable);

    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(:lockKey))", nativeQuery = true)
    void acquireTransactionLock(@Param("lockKey") String lockKey);

    Optional<AdminAuditLogEntity> findFirstByActorIdAndActionAndTargetIdOrderByCreatedAtDesc(
            UUID actorId,
            String action,
            UUID targetId
    );
}
