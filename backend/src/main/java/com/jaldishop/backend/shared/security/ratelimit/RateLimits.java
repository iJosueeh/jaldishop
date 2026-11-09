package com.jaldishop.backend.shared.security.ratelimit;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Fixed windows, anchored at first request. No state is evicted while its limit is active. */
@Component
public class RateLimits {
    private record Window(long expires, int count) {}
    private final Map<String, Window> windows = new HashMap<>();
    private final LongSupplier time;
    private final Environment environment;

    @org.springframework.beans.factory.annotation.Autowired
    public RateLimits(Environment environment) { this(environment, System::nanoTime); }
    RateLimits(Environment environment, LongSupplier time) {
        this.environment = environment;
        this.time = time;
    }

    public synchronized void check(String policy, String identity, int defaultLimit, Duration defaultWindow) {
        int limit = environment.getProperty("app.security.rate-limit." + policy + ".limit", Integer.class, defaultLimit);
        Duration duration = Duration.ofSeconds(environment.getProperty("app.security.rate-limit." + policy + ".window-seconds", Long.class, defaultWindow.toSeconds()));
        int capacity = environment.getProperty("app.security.rate-limit.max-entries", Integer.class, 10000);
        if (limit < 1 || duration.isNegative() || duration.isZero() || capacity < 1) {
            throw new IllegalStateException("Invalid rate limit configuration");
        }
        long now = time.getAsLong();
        windows.entrySet().removeIf(entry -> now - entry.getValue().expires() >= 0);
        String key = policy + ":" + identity;
        Window window = windows.get(key);
        if (window == null) {
            // Fail closed at capacity rather than evicting an attacker's active counters.
            if (windows.size() >= capacity) {
                long earliest = windows.values().stream().mapToLong(Window::expires).min().orElse(now);
                throw new RateLimitExceeded(seconds(earliest - now));
            }
            window = new Window(now + duration.toNanos(), 0);
        }
        if (window.count() >= limit) throw new RateLimitExceeded(seconds(window.expires() - now));
        windows.put(key, new Window(window.expires(), window.count() + 1));
    }

    private long seconds(long nanos) { return Math.max(1, (nanos + 999999999L) / 1000000000L); }
    synchronized int size() { return windows.size(); }
}
