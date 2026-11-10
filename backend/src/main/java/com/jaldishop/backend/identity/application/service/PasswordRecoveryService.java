package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.port.PasswordRecoveryRepository;
import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class PasswordRecoveryService {
    private final PasswordRecoveryRepository repository;
    private final PasswordResetTokens tokens;
    private final PasswordEncoder encoder;
    private final PasswordResetMail mail;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public PasswordRecoveryService(PasswordRecoveryRepository repository, PasswordResetTokens tokens,
            PasswordEncoder encoder, PasswordResetMail mail, TransactionTemplate transaction,
            @Qualifier("passwordRecoveryClock") Clock clock) {
        this.repository = repository; this.tokens = tokens; this.encoder = encoder;
        this.mail = mail; this.transaction = transaction; this.clock = clock;
    }

    public void issue(String email) {
        String token = tokens.generate();
        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(15));
        // Send only after commit. No HTTP caller observes account lookup or mail provider latency/failures.
        String recipient = transaction.execute(status -> {
            var user = repository.lockByEmail(email.trim().toLowerCase(Locale.ROOT));
            if (user.isEmpty() || !user.get().canAuthenticate()) return null;
            repository.replaceToken(user.get().getId(), tokens.hash(token), expiresAt);
            return user.get().getEmail();
        });
        if (recipient != null) mail.send(recipient, token, expiresAt);
    }

    public void reset(String token, String password) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalidToken();
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("La contraseña debe contener al menos 8 caracteres y como máximo 72 bytes UTF-8.");
        String hash = tokens.hash(token);
        transaction.executeWithoutResult(status -> {
            var id = repository.findUserId(hash).orElseThrow(PasswordRecoveryService::invalidToken);
            var user = repository.lockById(id).orElseThrow(PasswordRecoveryService::invalidToken);
            Instant now = clock.instant();
            if (!user.canAuthenticate() || !repository.consume(id, hash, now)) throw invalidToken();
            user.resetPassword(encoder.encode(password));
            repository.updateCredentials(user);
        });
    }

    private static BusinessRuleException invalidToken() {
        return new BusinessRuleException("INVALID_PASSWORD_RESET_TOKEN", "El token de recuperación no es válido o ha vencido.");
    }
}
