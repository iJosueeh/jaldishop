package com.jaldishop.backend.identity.web.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        UUID actorId,
        String actorEmail,
        String action,
        String targetType,
        UUID targetId,
        String status,
        String ipAddress,
        String details,
        Instant createdAt
) {}
