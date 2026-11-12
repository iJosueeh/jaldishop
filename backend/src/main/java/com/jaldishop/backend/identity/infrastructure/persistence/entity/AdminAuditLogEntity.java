package com.jaldishop.backend.identity.infrastructure.persistence.entity;

import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLogEntity {

    @Id
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_email", length = 254)
    private String actorEmail;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "target_type", nullable = false, length = 40)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminAuditStatus status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AdminAuditLogEntity() {}

    public AdminAuditLogEntity(
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
        this.id = id;
        this.actorId = actorId;
        this.actorEmail = actorEmail;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.status = status;
        this.ipAddress = ipAddress;
        this.details = details;
        this.createdAt = createdAt;
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
