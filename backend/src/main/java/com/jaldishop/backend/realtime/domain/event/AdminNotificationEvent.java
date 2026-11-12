package com.jaldishop.backend.realtime.domain.event;

import java.time.Instant;
import java.util.UUID;

public record AdminNotificationEvent(
        UUID eventId,
        String eventType,
        String resourceType,
        UUID resourceId,
        String action,
        Instant occurredAt,
        String summary
) {
    public static AdminNotificationEvent of(
            String eventType,
            String resourceType,
            UUID resourceId,
            String action,
            String summary
    ) {
        return new AdminNotificationEvent(
                UUID.randomUUID(),
                eventType,
                resourceType,
                resourceId,
                action,
                Instant.now(),
                summary
        );
    }
}
