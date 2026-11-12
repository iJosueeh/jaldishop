package com.jaldishop.backend.shared.security;

import java.util.Set;
import java.util.UUID;

public record JwtPrincipal(UUID userId, Set<String> roles, Long credentialsVersion, Boolean primaryAdmin) {
    public JwtPrincipal {
        // Keep MVC constructor binding compatible when no principal resolver is installed (standalone tests).
        credentialsVersion = credentialsVersion == null ? 0L : credentialsVersion;
        primaryAdmin = primaryAdmin != null && primaryAdmin;
    }
    public JwtPrincipal(UUID userId, Set<String> roles) { this(userId, roles, 0L, false); }
    public JwtPrincipal(UUID userId, Set<String> roles, Long credentialsVersion) { this(userId, roles, credentialsVersion, false); }
    public boolean isPrimaryAdmin() { return Boolean.TRUE.equals(primaryAdmin); }
}
