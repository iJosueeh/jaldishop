package com.jaldishop.backend.identity.application.port;

import com.jaldishop.backend.identity.domain.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** All operations must participate in the caller's transaction. Lock user before changing its token. */
public interface PasswordRecoveryRepository {
    Optional<User> lockByEmail(String email);
    Optional<User> lockById(UUID id);
    Optional<UUID> findUserId(String tokenHash);
    void replaceToken(UUID userId, String tokenHash, Instant expiresAt);
    boolean consume(UUID userId, String tokenHash, Instant now);
    void updateCredentials(User user);
}
