package com.jaldishop.backend.identity.domain.repository;

import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface AdminAuditLogRepository {
    AdminAuditLog save(AdminAuditLog auditLog);
    Page<AdminAuditLog> searchLogs(
            String action,
            AdminAuditStatus status,
            UUID actorId,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    );
}
