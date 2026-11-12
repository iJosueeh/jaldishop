package com.jaldishop.backend.identity.domain.model;

import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AdminAuditLog {

    private final UUID id;
    private final UUID actorId;
    private final String actorEmail;
    private final String action;
    private final String targetType;
    private final UUID targetId;
    private final AdminAuditStatus status;
    private final String ipAddress;
    private final String details;
    private final Instant createdAt;

    public AdminAuditLog(
            UUID id,
            UUID actorId,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            AdminAuditStatus status,
            String ipAddress,
            String details,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id no puede ser nulo");
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = Objects.requireNonNull(action, "action no puede ser nulo");
        this.targetType = Objects.requireNonNull(targetType, "targetType no puede ser nulo");
        this.targetId = targetId;
        this.status = Objects.requireNonNull(status, "status no puede ser nulo");
        this.ipAddress = ipAddress;
        this.details = details;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt no puede ser nulo");
    }

    public static AdminAuditLog record(
            UUID actorId,
            String actorEmail,
            String action,
            String targetType,
            UUID targetId,
            AdminAuditStatus status,
            String ipAddress,
            String details
    ) {
        return new AdminAuditLog(
                UUID.randomUUID(),
                actorId,
                actorEmail,
                action,
                targetType,
                targetId,
                status,
                ipAddress,
                details,
                Instant.now()
        );
    }

    public UUID getId() { return id; }
    public UUID getActorId() { return actorId; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public AdminAuditStatus getStatus() { return status; }
    public String getIpAddress() { return ipAddress; }
    public String getDetails() { return details; }
    public Instant getCreatedAt() { return createdAt; }
}
