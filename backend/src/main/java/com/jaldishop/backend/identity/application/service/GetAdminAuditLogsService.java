package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import com.jaldishop.backend.identity.domain.repository.AdminAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminAuditLogsService {

    private final AdminAuditLogRepository auditLogRepository;

    public GetAdminAuditLogsService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public Page<AdminAuditLog> execute(
            String action,
            AdminAuditStatus status,
            UUID actorId,
            Instant startDate,
            Instant endDate,
            int page,
            int size
    ) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = (size <= 0 || size > 100) ? 20 : size;

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.searchLogs(action, status, actorId, startDate, endDate, pageable);
    }
}
