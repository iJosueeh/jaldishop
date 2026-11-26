package com.jaldishop.backend.identity.infrastructure.persistence.adapter;

import com.jaldishop.backend.identity.application.port.EmailVerificationRepository;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailVerificationJdbcAdapter implements EmailVerificationRepository {

    private final JdbcClient jdbc;
    private final UserRepository users;

    public EmailVerificationJdbcAdapter(JdbcClient jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }

    @Override
    public Optional<User> lockByEmail(String email) {
        return jdbc.sql("SELECT id FROM users WHERE email = :email FOR UPDATE")
                .param("email", email)
                .query(UUID.class)
                .optional()
                .flatMap(users::findById);
    }

    @Override
    public Optional<User> lockById(UUID id) {
        return jdbc.sql("SELECT id FROM users WHERE id = :id FOR UPDATE")
                .param("id", id)
                .query(UUID.class)
                .optional()
                .flatMap(users::findById);
    }

    @Override
    public Optional<UUID> findUserId(String tokenHash) {
        return jdbc.sql("SELECT user_id FROM email_verification_tokens WHERE token_hash = :hash")
                .param("hash", tokenHash)
                .query(UUID.class)
                .optional();
    }

    @Override
    public Optional<Instant> findTokenCreatedAt(UUID userId) {
        return jdbc.sql("SELECT created_at FROM email_verification_tokens WHERE user_id = :id")
                .param("id", userId)
                .query(Timestamp.class)
                .optional()
                .map(Timestamp::toInstant);
    }

    @Override
    public void replaceToken(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        jdbc.sql("""
                INSERT INTO email_verification_tokens(user_id, token_hash, expires_at, created_at)
                VALUES (:id, :hash, :expires, :created)
                ON CONFLICT (user_id) DO UPDATE SET
                    token_hash = EXCLUDED.token_hash,
                    expires_at = EXCLUDED.expires_at,
                    created_at = EXCLUDED.created_at
                """)
                .param("id", userId)
                .param("hash", tokenHash)
                .param("expires", Timestamp.from(expiresAt))
                .param("created", Timestamp.from(createdAt))
                .update();
    }

    @Override
    public boolean consume(UUID userId, String tokenHash, Instant now) {
        return jdbc.sql("DELETE FROM email_verification_tokens WHERE user_id = :id AND token_hash = :hash AND expires_at > :now")
                .param("id", userId)
                .param("hash", tokenHash)
                .param("now", Timestamp.from(now))
                .update() == 1;
    }

    @Override
    public void markEmailVerified(User user) {
        int updated = jdbc.sql("""
                UPDATE users
                SET email_verified = TRUE,
                    email_verified_at = :verifiedAt,
                    updated_at = :updatedAt
                WHERE id = :id
                """)
                .param("verifiedAt", Timestamp.from(user.getEmailVerifiedAt()))
                .param("updatedAt", Timestamp.from(user.getUpdatedAt()))
                .param("id", user.getId())
                .update();
        if (updated != 1) {
            throw new IllegalStateException("No se pudo marcar el correo como verificado.");
        }
    }
}
