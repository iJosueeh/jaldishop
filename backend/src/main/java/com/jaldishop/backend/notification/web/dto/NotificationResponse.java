package com.jaldishop.backend.notification.web.dto;

import com.jaldishop.backend.notification.domain.model.Notification;
import com.jaldishop.backend.notification.domain.enums.NotificationStatus;
import com.jaldishop.backend.notification.domain.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        NotificationType type,
        String title,
        String message,
        NotificationStatus status,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse fromDomain(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getReadAt());
    }
}
