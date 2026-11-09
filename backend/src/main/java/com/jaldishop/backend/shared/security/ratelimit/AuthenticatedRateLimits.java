package com.jaldishop.backend.shared.security.ratelimit;

import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Selective policies; callers supply the identity established by JWT authentication. */
@Component
public class AuthenticatedRateLimits {
    private final RateLimits limits;

    public AuthenticatedRateLimits(RateLimits limits) { this.limits = limits; }

    public void reservationCreation(UUID userId) {
        limits.checkAuthenticated("reservation-create-user", userId, 12, Duration.ofMinutes(1));
    }

    public void paymentCreation(UUID userId) {
        limits.checkAuthenticated("payment-create-user", userId, 6, Duration.ofMinutes(1));
    }

    public void adminPaymentOperation(UUID userId) {
        limits.checkAuthenticated("payment-admin-remote-user", userId, 10, Duration.ofMinutes(1));
    }
}
