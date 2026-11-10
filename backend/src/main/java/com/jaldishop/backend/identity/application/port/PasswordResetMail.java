package com.jaldishop.backend.identity.application.port;

import java.time.Instant;

public interface PasswordResetMail {
    void send(String recipient, String token, Instant expiresAt);
}
