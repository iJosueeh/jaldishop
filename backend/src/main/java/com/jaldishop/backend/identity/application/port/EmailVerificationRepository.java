package com.jaldishop.backend.identity.application.port;

import com.jaldishop.backend.identity.domain.model.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** All operations must participate in the caller's transaction. Lock user before changing its token. */
public interface EmailVerificationRepository {
    Optional<User> lockByEmail(String email);
    Optional<User> lockById(UUID id);
    Optional<UUID> findUserId(String tokenHash);
    Optional<Instant> findTokenCreatedAt(UUID userId);
    void replaceToken(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt);
    boolean consume(UUID userId, String tokenHash, Instant now);
    void markEmailVerified(User user);
}
