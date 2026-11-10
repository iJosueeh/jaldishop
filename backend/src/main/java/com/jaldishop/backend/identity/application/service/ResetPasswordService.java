package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.port.PasswordRecoveryRepository;
import com.jaldishop.backend.identity.application.command.ResetPasswordCommand;
import com.jaldishop.backend.identity.infrastructure.security.PasswordResetTokens;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

@Service
public class ResetPasswordService {
    private final PasswordRecoveryRepository repository;
    private final PasswordResetTokens tokens;
    private final PasswordEncoder encoder;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ResetPasswordService(PasswordRecoveryRepository repository, PasswordResetTokens tokens,
            PasswordEncoder encoder, TransactionTemplate transaction,
            @Qualifier("passwordRecoveryClock") Clock clock) {
        this.repository = repository;
        this.tokens = tokens;
        this.encoder = encoder;
        this.transaction = transaction;
        this.clock = clock;
    }

    public void execute(ResetPasswordCommand command) {
        String token = command.token();
        String password = command.newPassword();
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalidToken();
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("La contraseña debe contener al menos 8 caracteres y como máximo 72 bytes UTF-8.");
        String hash = tokens.hash(token);
        transaction.executeWithoutResult(status -> {
            var id = repository.findUserId(hash).orElseThrow(ResetPasswordService::invalidToken);
            var user = repository.lockById(id).orElseThrow(ResetPasswordService::invalidToken);
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
