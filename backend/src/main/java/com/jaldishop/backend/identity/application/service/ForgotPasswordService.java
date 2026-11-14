package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.ForgotPasswordCommand;
import com.jaldishop.backend.identity.application.port.PasswordRecoveryRepository;
import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import com.jaldishop.backend.identity.infrastructure.security.PasswordResetTokens;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
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
import java.util.Set;
import java.util.stream.Collectors;

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
        String origin = command.origin();
        // Applied to every address, including unknown addresses; never expose the account counter.
        try { limits.check("forgot-password-account", tokens.hash(normalized), 3, Duration.ofMinutes(15)); }
        catch (RateLimitExceeded exceeded) { return; }
        try {
            executor.execute(() -> {
                try { issue(normalized, origin); }
                catch (RuntimeException failure) {
                    // Providers may embed mail bodies or tokens in exceptions.
                    log.warn("No se pudo procesar una solicitud de recuperación de contraseña.");
                }
            });
        } catch (TaskRejectedException full) { throw new RateLimitExceeded(60); }
    }

    private void issue(String email, String origin) {
        String token = tokens.generate();
        Instant expiresAt = clock.instant().plus(Duration.ofMinutes(15));
        // Send only after commit, outside the HTTP request and the user row lock.
        record RecipientInfo(String email, Set<RoleName> roles) {}
        RecipientInfo recipient = transaction.execute(status -> {
            var user = repository.lockByEmail(email);
            if (user.isEmpty() || !user.get().canAuthenticate()) return null;
            repository.replaceToken(user.get().getId(), tokens.hash(token), expiresAt);
            Set<RoleName> roles = user.get().getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());
            return new RecipientInfo(user.get().getEmail(), roles);
        });
        if (recipient != null) mail.send(recipient.email(), token, expiresAt, recipient.roles(), origin);
    }
}
