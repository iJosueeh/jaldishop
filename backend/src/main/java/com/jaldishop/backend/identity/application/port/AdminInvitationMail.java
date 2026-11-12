package com.jaldishop.backend.identity.application.port;

import java.time.Instant;

public interface AdminInvitationMail {
    void sendInvitation(String recipientEmail, String recipientName, String rawToken, Instant expiresAt);
}
