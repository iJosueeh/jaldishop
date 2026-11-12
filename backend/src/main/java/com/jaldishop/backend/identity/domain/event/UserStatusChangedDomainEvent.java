package com.jaldishop.backend.identity.domain.event;

import java.util.UUID;

public record UserStatusChangedDomainEvent(
        UUID userId,
        String targetStatus,
        UUID changedBy
) {}
