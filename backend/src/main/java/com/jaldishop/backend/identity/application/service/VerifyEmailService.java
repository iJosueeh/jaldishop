package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.VerifyEmailCommand;
import com.jaldishop.backend.identity.application.port.EmailVerificationRepository;
import com.jaldishop.backend.identity.infrastructure.security.OpaqueTokenCodec;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;

@Service
public class VerifyEmailService {

    private final EmailVerificationRepository repository;
    private final OpaqueTokenCodec tokens;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public VerifyEmailService(
            EmailVerificationRepository repository,
            OpaqueTokenCodec tokens,
            TransactionTemplate transaction,
            @Qualifier("passwordRecoveryClock") Clock clock
    ) {
        this.repository = repository;
        this.tokens = tokens;
        this.transaction = transaction;
        this.clock = clock;
    }

    public void execute(VerifyEmailCommand command) {
        String token = command.token() != null ? command.token().trim() : null;
        if (!tokens.isValidFormat(token)) {
            throw invalidToken();
        }
        String hash = tokens.hash(token);
        transaction.executeWithoutResult(status -> {
            var userId = repository.findUserId(hash).orElseThrow(VerifyEmailService::invalidToken);
            var user = repository.lockById(userId).orElseThrow(VerifyEmailService::invalidToken);
            Instant now = clock.instant();
            if (!user.canAuthenticate() || !repository.consume(userId, hash, now)) {
                throw invalidToken();
            }
            user.verifyEmail(now);
            repository.markEmailVerified(user);
        });
    }

    private static BusinessRuleException invalidToken() {
        return new BusinessRuleException(
                "INVALID_EMAIL_VERIFICATION_TOKEN",
                "El enlace de verificación no es válido o ha vencido."
        );
    }
}
