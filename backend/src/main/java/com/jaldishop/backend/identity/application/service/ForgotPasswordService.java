package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.ForgotPasswordCommand;
import com.jaldishop.backend.identity.application.port.PasswordRecoveryRepository;
import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import com.jaldishop.backend.identity.infrastructure.security.PasswordResetTokens;
import com.jaldishop.backend.shared.security.ratelimit.RateLimits;
import com.jaldishop.backend.shared.security.ratelimit.RateLimitExceeded;
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
public class ForgotPasswordService {
    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordService.class);

    private final RateLimits limits;
    private final PasswordResetTokens tokens;
    private final TaskExecutor executor;
    private final PasswordRecoveryRepository repository;
    private final PasswordResetMail mail;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public ForgotPasswordService(RateLimits limits, PasswordResetTokens tokens,
            @Qualifier("passwordRecoveryExecutor") TaskExecutor executor,
            PasswordRecoveryRepository repository, PasswordResetMail mail, TransactionTemplate transaction,
            @Qualifier("passwordRecoveryClock") Clock clock) {
        this.limits = limits;
        this.tokens = tokens;
        this.executor = executor;
        this.repository = repository;
        this.mail = mail;
        this.transaction = transaction;
        this.clock = clock;
    }

    public void execute(ForgotPasswordCommand command) {
        String normalized = command.email().trim().toLowerCase(Locale.ROOT);
        // Applied to every address, including unknown addresses; never expose the account counter.
        try { limits.check("forgot-password-account", tokens.hash(normalized), 3, Duration.ofMinutes(15)); }
        catch (RateLimitExceeded exceeded) { return; }
        try {
            executor.execute(() -> {
                try { issue(normalized); }
                catch (RuntimeException failure) {
                    // Providers may embed mail bodies or tokens in exceptions.
                    log.warn("No se pudo procesar una solicitud de recuperación de contraseña.");
                }
            });
        } catch (TaskRejectedException full) { throw new RateLimitExceeded(60); }
    }

    private void issue(String email) {
        String token = tokens.generate();
        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(15));
        // Send only after commit, outside the HTTP request and the user row lock.
        String recipient = transaction.execute(status -> {
            var user = repository.lockByEmail(email);
            if (user.isEmpty() || !user.get().canAuthenticate()) return null;
            repository.replaceToken(user.get().getId(), tokens.hash(token), expiresAt);
            return user.get().getEmail();
        });
        if (recipient != null) mail.send(recipient, token, expiresAt);
    }
}
