package com.jaldishop.backend.identity.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.infrastructure.persistence.entity.AdminAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AdminAuditLogJpaRepository extends JpaRepository<AdminAuditLogEntity, UUID>, JpaSpecificationExecutor<AdminAuditLogEntity> {
}
