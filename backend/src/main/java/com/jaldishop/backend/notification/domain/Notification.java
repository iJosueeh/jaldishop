package com.jaldishop.backend.notification.domain;

import com.jaldishop.backend.shared.exception.BusinessRuleException;

import java.time.Instant;
import java.util.UUID;

public class Notification {

    private final UUID id;
    private final UUID userId;
    private final NotificationType type;
    private final String title;
    private final String message;
    private NotificationStatus status;
    private final Instant createdAt;
    private Instant readAt;

    private Notification(UUID id, UUID userId, NotificationType type, String title, String message,
                         NotificationStatus status, Instant createdAt, Instant readAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public static Notification create(UUID userId, NotificationType type, String title, String message) {
        if (userId == null) {
            throw new IllegalArgumentException("userId no puede ser nulo.");
        }
        if (type == null) {
            throw new IllegalArgumentException("type no puede ser nulo.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title no puede ser nulo o vacío.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message no puede ser nulo o vacío.");
        }
        Instant now = Instant.now();
        return new Notification(UUID.randomUUID(), userId, type, title.trim(), message.trim(),
                NotificationStatus.UNREAD, now, null);
    }

    public static Notification reconstitute(UUID id, UUID userId, NotificationType type, String title,
                                            String message, NotificationStatus status, Instant createdAt,
                                            Instant readAt) {
        return new Notification(id, userId, type, title, message, status, createdAt, readAt);
    }

    public void markAsRead() {
        if (this.status == NotificationStatus.READ) {
            throw new BusinessRuleException("NOTIFICATION_ALREADY_READ", "La notificación ya fue marcada como leída.");
        }
        this.status = NotificationStatus.READ;
        this.readAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public NotificationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public boolean isUnread() { return status == NotificationStatus.UNREAD; }
}
