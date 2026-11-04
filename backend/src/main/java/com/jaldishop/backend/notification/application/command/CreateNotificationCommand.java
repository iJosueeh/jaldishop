package com.jaldishop.backend.notification.application.command;

import com.jaldishop.backend.notification.domain.enums.NotificationType;

import java.util.UUID;

public record CreateNotificationCommand(
        UUID userId,
        NotificationType type,
        String title,
        String message,
        String deduplicationKey
) {
    public CreateNotificationCommand(UUID userId, NotificationType type, String title, String message) {
        this(userId, type, title, message, null);
    }
}
