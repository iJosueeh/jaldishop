package com.jaldishop.backend.shared.security.ratelimit;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Fixed windows with per-key atomic counters and bounded capacity protection.
 * Independent stores for public vs authenticated requests prevent resource starvation across boundaries.
 */
@Component
public class RateLimits {

    private static final class CounterWindow {
        final long expires;
        final AtomicInteger count = new AtomicInteger(1);

        CounterWindow(long expires) {
            this.expires = expires;
        }
    }

    private static final class Store {
        final ConcurrentHashMap<String, CounterWindow> map = new ConcurrentHashMap<>();
        final AtomicLong lastCleanup = new AtomicLong(0);
        long earliestExpiration = 0;
        // Throttle background sweeps
        static final long CLEANUP_INTERVAL_NANOS = Duration.ofMillis(250).toNanos();
    }

    private final Store publicStore = new Store();
    private final Store authenticatedStore = new Store();
    private final LongSupplier time;
    private final Environment environment;

    @org.springframework.beans.factory.annotation.Autowired
    public RateLimits(Environment environment) {
        this(environment, System::nanoTime);
    }

    RateLimits(Environment environment, LongSupplier time) {
        this.environment = environment;
        this.time = time;
    }

    public void check(String policy, String identity, int defaultLimit, Duration defaultWindow) {
        check(publicStore, "max-entries", policy, identity, defaultLimit, defaultWindow);
    }

    /** Call only with the identity established by JWT authentication, never request-supplied IDs. */
    public void checkAuthenticated(String policy, UUID userId, int defaultLimit, Duration defaultWindow) {
        check(authenticatedStore, "authenticated-max-entries", policy,
                java.util.Objects.requireNonNull(userId).toString(), defaultLimit, defaultWindow);
    }

    private void check(Store store, String capacityProperty, String policy,
                       String identity, int defaultLimit, Duration defaultWindow) {
        int limit = environment.getProperty("app.security.rate-limit." + policy + ".limit", Integer.class, defaultLimit);
        Duration duration = Duration.ofSeconds(environment.getProperty(
                "app.security.rate-limit." + policy + ".window-seconds", Long.class, defaultWindow.toSeconds()));
        int capacity = environment.getProperty("app.security.rate-limit." + capacityProperty, Integer.class, 10000);

        if (limit < 1 || duration.isNegative() || duration.isZero() || capacity < 1) {
            throw new IllegalStateException("Invalid rate limit configuration");
        }

        long windowDurationNanos = duration.toNanos();
        String key = policy + ":" + identity;

        while (true) {
            long now = time.getAsLong();
            cleanupIfNecessary(store, now);

            CounterWindow window = store.map.get(key);

            if (window != null) {
                long remaining = window.expires - now;
                if (remaining <= 0) {
                    // Window expired: atomically remove old window and retry
                    store.map.remove(key, window);
                    continue;
                }

                // Window is active: atomically attempt to increment
                while (true) {
                    long currentNow = time.getAsLong();
                    long currentRemaining = window.expires - currentNow;
                    if (currentRemaining <= 0) {
                        // Window expired while in contention: evict and retry
                        store.map.remove(key, window);
                        break;
                    }

                    int current = window.count.get();
                    if (current >= limit) {
                        throw new RateLimitExceeded(seconds(currentRemaining));
                    }
                    if (window.count.compareAndSet(current, current + 1)) {
                        return;
                    }
                }
            } else {
                // Key not present: enforce hard capacity before admitting new key
                synchronized (store) {
                    long currentNow = time.getAsLong();
                    // Double check inside store lock
                    window = store.map.get(key);
                    if (window != null) {
                        // Another thread already inserted while waiting for lock
                        continue;
                    }

                    if (store.map.size() >= capacity) {
                        // Fast path: if known earliest expiration has not elapsed, map remains full
                        if (store.earliestExpiration > currentNow) {
                            throw new RateLimitExceeded(seconds(store.earliestExpiration - currentNow));
                        }

                        sweepExpired(store, currentNow);
                        if (store.map.size() >= capacity) {
                            store.earliestExpiration = store.map.values().stream()
                                    .mapToLong(w -> w.expires)
                                    .min()
                                    .orElse(currentNow);
                            throw new RateLimitExceeded(seconds(store.earliestExpiration - currentNow));
                        }
                    }

                    // Insert new window starting with count = 1
                    store.earliestExpiration = 0;
                    store.map.put(key, new CounterWindow(currentNow + windowDurationNanos));
                    return;
                }
            }
        }
    }

    private void cleanupIfNecessary(Store store, long now) {
        long last = store.lastCleanup.get();
        if (last == 0 || now - last > Store.CLEANUP_INTERVAL_NANOS) {
            if (store.lastCleanup.compareAndSet(last, now)) {
                sweepExpired(store, now);
            }
        }
    }

    private void sweepExpired(Store store, long now) {
        store.map.entrySet().removeIf(entry -> now - entry.getValue().expires >= 0);
    }

    private long seconds(long nanos) {
        return Math.max(1, (nanos + 999999999L) / 1000000000L);
    }

    int size() {
        return publicStore.map.size();
    }
}
