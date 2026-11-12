package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.service.GetAdminAuditLogsService;
import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import com.jaldishop.backend.identity.web.dto.AdminAuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN') and principal.primaryAdmin")
@Validated
public class AdminAuditLogController {

    private final GetAdminAuditLogsService getAdminAuditLogsService;

    public AdminAuditLogController(GetAdminAuditLogsService getAdminAuditLogsService) {
        this.getAdminAuditLogsService = getAdminAuditLogsService;
    }

    @GetMapping
    public ResponseEntity<Page<AdminAuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) AdminAuditStatus status,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AdminAuditLog> logs = getAdminAuditLogsService.execute(
                action,
                status,
                actorId,
                startDate,
                endDate,
                page,
                size
        );

        Page<AdminAuditLogResponse> response = logs.map(log -> new AdminAuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorEmail(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getStatus().name(),
                log.getIpAddress(),
                log.getDetails(),
                log.getCreatedAt()
        ));

        return ResponseEntity.ok(response);
    }
}
