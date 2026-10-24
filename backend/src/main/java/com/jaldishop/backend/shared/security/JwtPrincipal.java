package com.jaldishop.backend.shared.security;

import java.util.Set;
import java.util.UUID;

public record JwtPrincipal(
        UUID userId,
        Set<String> roles
) {}
