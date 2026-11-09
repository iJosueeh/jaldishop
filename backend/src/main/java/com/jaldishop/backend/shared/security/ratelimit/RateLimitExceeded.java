package com.jaldishop.backend.shared.security.ratelimit;

public class RateLimitExceeded extends RuntimeException {
    private final long retryAfter;
    public RateLimitExceeded(long retryAfter) {
        super("Demasiadas solicitudes. Intente nuevamente más tarde.");
        this.retryAfter = retryAfter;
    }
    public long retryAfter() { return retryAfter; }
}
