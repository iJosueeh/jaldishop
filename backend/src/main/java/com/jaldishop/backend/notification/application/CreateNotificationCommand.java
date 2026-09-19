package com.jaldishop.backend.notification.application;

import com.jaldishop.backend.notification.domain.NotificationType;

import java.util.UUID;

public record CreateNotificationCommand(
        UUID userId,
        NotificationType type,
        String title,
        String message
) {}
