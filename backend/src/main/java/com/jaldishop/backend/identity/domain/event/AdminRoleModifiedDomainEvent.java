package com.jaldishop.backend.identity.domain.event;

import java.util.UUID;

public record AdminRoleModifiedDomainEvent(
        UUID userId,
        String email,
        String action, // "PROMOTED" or "REVOKED"
        UUID performedBy
) {}
