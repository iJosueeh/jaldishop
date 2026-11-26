package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.ResendVerificationCommand;
import com.jaldishop.backend.identity.application.port.EmailVerificationMail;
import com.jaldishop.backend.identity.application.port.EmailVerificationRepository;
import com.jaldishop.backend.identity.infrastructure.security.OpaqueTokenCodec;
import com.jaldishop.backend.shared.security.ratelimit.RateLimitExceeded;
import com.jaldishop.backend.shared.security.ratelimit.RateLimits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class ResendVerificationEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendVerificationEmailService.class);
    static final Duration TOKEN_TTL = Duration.ofHours(24);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private final RateLimits limits;
    private final OpaqueTokenCodec tokens;
    private final TaskExecutor executor;
    private final EmailVerificationRepository repository;
    private final EmailVerificationMail mail;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ResendVerificationEmailService(
            RateLimits limits,
            OpaqueTokenCodec tokens,
            @Qualifier("passwordRecoveryExecutor") TaskExecutor executor,
            EmailVerificationRepository repository,
            EmailVerificationMail mail,
            TransactionTemplate transaction,
            @Qualifier("passwordRecoveryClock") Clock clock
    ) {
        this.limits = limits;
        this.tokens = tokens;
        this.executor = executor;
        this.repository = repository;
        this.mail = mail;
        this.transaction = transaction;
        this.clock = clock;
    }

    public void execute(ResendVerificationCommand command) {
        String normalized = command.email().trim().toLowerCase(Locale.ROOT);
        // Applied to every address, including unknown/verified addresses; never expose account state.
        try {
            limits.check("resend-verification-account", tokens.hash(normalized), 3, Duration.ofMinutes(15));
        } catch (RateLimitExceeded exceeded) {
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    issue(normalized);
                } catch (RuntimeException failure) {
                    // Providers may embed mail bodies or tokens in exceptions.
                    log.warn("No se pudo procesar una solicitud de reenvío de verificación de correo.");
                }
            });
        } catch (TaskRejectedException full) {
            throw new RateLimitExceeded(60);
        }
    }

    private void issue(String email) {
        String token = tokens.generate();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(TOKEN_TTL);
        record RecipientInfo(String email, String firstName) {}

        RecipientInfo recipient = transaction.execute(status -> {
            var userOpt = repository.lockByEmail(email);
            if (userOpt.isEmpty()) {
                return null;
            }
            var user = userOpt.get();
            if (!user.canAuthenticate() || user.isEmailVerified()) {
                return null;
            }
            var lastCreated = repository.findTokenCreatedAt(user.getId());
            if (lastCreated.isPresent() && lastCreated.get().plus(RESEND_COOLDOWN).isAfter(now)) {
                return null;
            }
            repository.replaceToken(user.getId(), tokens.hash(token), expiresAt, now);
            return new RecipientInfo(user.getEmail(), user.getFirstName());
        });

        if (recipient != null) {
            mail.sendVerification(recipient.email(), recipient.firstName(), token, expiresAt);
        }
    }
}
