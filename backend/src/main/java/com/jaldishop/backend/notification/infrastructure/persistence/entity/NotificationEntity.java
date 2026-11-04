package com.jaldishop.backend.notification.infrastructure.persistence.entity;

import com.jaldishop.backend.notification.domain.enums.NotificationStatus;
import com.jaldishop.backend.notification.domain.enums.NotificationType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "deduplication_key", length = 100, unique = true)
    private String deduplicationKey;

    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationEntity() {}

    public NotificationEntity(UUID id, UUID userId, NotificationType type, String title, String message,
                              NotificationStatus status, Instant createdAt, Instant readAt) {
        this(id, userId, type, title, message, null, status, createdAt, readAt);
    }

    public NotificationEntity(UUID id, UUID userId, NotificationType type, String title, String message,
                              String deduplicationKey, NotificationStatus status, Instant createdAt, Instant readAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.deduplicationKey = deduplicationKey;
        this.status = status;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
