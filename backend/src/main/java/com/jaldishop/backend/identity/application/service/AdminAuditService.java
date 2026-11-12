package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import com.jaldishop.backend.identity.domain.repository.AdminAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    private final AdminAuditLogRepository auditLogRepository;

    public AdminAuditService(AdminAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logSuccess(
            UUID actorId,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            String ipAddress,
            String details
    ) {
        saveAudit(actorId, actorEmail, action, targetType, targetId, AdminAuditStatus.SUCCESS, ipAddress, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(
            UUID actorId,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            String ipAddress,
            String details
    ) {
        saveAudit(actorId, actorEmail, action, targetType, targetId, AdminAuditStatus.FAILURE, ipAddress, details);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDenied(
            UUID actorId,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            String ipAddress,
            String details
    ) {
        saveAudit(actorId, actorEmail, action, targetType, targetId, AdminAuditStatus.DENIED, ipAddress, details);
    }

    private void saveAudit(
            UUID actorId,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            AdminAuditStatus status,
            String ipAddress,
            String details
    ) {
        try {
            AdminAuditLog auditLog = AdminAuditLog.record(
                    actorId,
                    actorEmail,
                    action,
                    targetType,
                    targetId,
                    status,
                    ipAddress,
                    sanitize(details)
            );
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Fallo al persistir log de auditoría administrativa: {}", ex.getMessage(), ex);
        }
    }

    private String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        // Mask passwords, secrets, tokens if any inadvertently leak
        return input.replaceAll("(?i)(password|token|secret|jwt)=\\s*[^,;\\s]+", "$1=[REDACTED]");
    }
}
