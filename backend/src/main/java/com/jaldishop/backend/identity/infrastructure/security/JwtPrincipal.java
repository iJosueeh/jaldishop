package com.jaldishop.backend.identity.infrastructure.security;

import java.util.Set;

public record JwtPrincipal(
        Long userId,
        Set<String> roles
) {}
