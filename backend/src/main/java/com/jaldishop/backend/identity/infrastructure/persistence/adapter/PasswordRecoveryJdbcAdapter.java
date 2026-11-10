package com.jaldishop.backend.identity.infrastructure.persistence.adapter;

import com.jaldishop.backend.identity.application.port.PasswordRecoveryRepository;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PasswordRecoveryJdbcAdapter implements PasswordRecoveryRepository {
    private final JdbcClient jdbc;
    private final UserRepository users;
    public PasswordRecoveryJdbcAdapter(JdbcClient jdbc, UserRepository users) {
        this.jdbc = jdbc;
        this.users = users;
    }
    public Optional<User> lockByEmail(String email) {
        return jdbc.sql("SELECT id FROM users WHERE email = :email FOR UPDATE")
                .param("email", email).query(UUID.class).optional().flatMap(users::findById);
    }
    public Optional<User> lockById(UUID id) {
        return jdbc.sql("SELECT id FROM users WHERE id = :id FOR UPDATE")
                .param("id", id).query(UUID.class).optional().flatMap(users::findById);
    }
    public Optional<UUID> findUserId(String hash) {
        return jdbc.sql("SELECT user_id FROM password_reset_tokens WHERE token_hash = :hash")
                .param("hash", hash).query(UUID.class).optional();
    }
    public void replaceToken(UUID id, String hash, Instant expiresAt) {
        jdbc.sql("""
                INSERT INTO password_reset_tokens(user_id, token_hash, expires_at)
                VALUES (:id, :hash, :expires)
                ON CONFLICT (user_id) DO UPDATE SET token_hash = EXCLUDED.token_hash, expires_at = EXCLUDED.expires_at
                """).param("id", id).param("hash", hash).param("expires", Timestamp.from(expiresAt)).update();
    }
    public boolean consume(UUID id, String hash, Instant now) {
        return jdbc.sql("DELETE FROM password_reset_tokens WHERE user_id = :id AND token_hash = :hash AND expires_at > :now")
                .param("id", id).param("hash", hash).param("now", Timestamp.from(now)).update() == 1;
    }
    public void updateCredentials(User user) {
        int updated = jdbc.sql("""
                UPDATE users SET password_encoded = :password, credentials_version = :version, updated_at = :updated
                WHERE id = :id AND credentials_version = :previous
                """).param("password", user.getPassword()).param("version", user.getCredentialsVersion())
                .param("updated", Timestamp.from(user.getUpdatedAt())).param("id", user.getId())
                .param("previous", user.getCredentialsVersion() - 1).update();
        if (updated != 1) throw new IllegalStateException("No se pudo actualizar las credenciales.");
    }
}
