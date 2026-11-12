package com.jaldishop.backend.identity.domain.event;

import java.util.UUID;

public record UserRegisteredDomainEvent(
        UUID userId,
        String email,
        String role
) {}
