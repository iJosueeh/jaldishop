package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.shared.security.ratelimit.RateLimits;
import com.jaldishop.backend.shared.security.ratelimit.RateLimitExceeded;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Locale;

@Service
public class RequestPasswordRecoveryService {
    private final RateLimits limits;
    private final PasswordResetTokens tokens;
    private final TaskExecutor executor;
    private final PasswordRecoveryService recovery;
    public RequestPasswordRecoveryService(RateLimits limits, PasswordResetTokens tokens,
            @Qualifier("passwordRecoveryExecutor") TaskExecutor executor, PasswordRecoveryService recovery) {
        this.limits = limits; this.tokens = tokens; this.executor = executor; this.recovery = recovery;
    }
    public void request(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        // Applied to every address, including unknown addresses; never expose the account counter.
        try { limits.check("forgot-password-account", tokens.hash(normalized), 3, Duration.ofMinutes(15)); }
        catch (RateLimitExceeded exceeded) { return; }
        try {
            executor.execute(() -> {
                try { recovery.issue(normalized); }
                catch (RuntimeException failure) {
                    // Do not log exception messages/stack traces: providers may embed mail bodies or tokens.
                    org.slf4j.LoggerFactory.getLogger(RequestPasswordRecoveryService.class)
                            .warn("No se pudo procesar una solicitud de recuperación de contraseña.");
                }
            });
        } catch (TaskRejectedException full) { throw new RateLimitExceeded(60); }
    }
}
