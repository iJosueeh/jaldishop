package com.jaldishop.backend.identity.infrastructure.security;

import java.util.Set;
import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        Set<String> roles
) {}
