package com.jaldishop.backend.identity.application.port;

import java.time.Instant;

public interface EmailVerificationMail {
    void sendVerification(String recipientEmail, String recipientName, String token, Instant expiresAt);
}
