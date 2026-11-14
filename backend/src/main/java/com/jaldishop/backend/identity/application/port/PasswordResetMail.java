package com.jaldishop.backend.identity.application.port;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import java.time.Instant;
import java.util.Set;

public interface PasswordResetMail {
    void send(String recipient, String token, Instant expiresAt);

    default void send(String recipient, String token, Instant expiresAt, Set<RoleName> roles, String origin) {
        send(recipient, token, expiresAt);
    }
}
