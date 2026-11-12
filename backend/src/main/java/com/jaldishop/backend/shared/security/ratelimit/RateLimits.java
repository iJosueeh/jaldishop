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
        int count = 1;

        CounterWindow(long expires) {
            this.expires = expires;
        }
    }

    private static final class Store {
        final ConcurrentHashMap<String, CounterWindow> map = new ConcurrentHashMap<>();
        final AtomicLong lastCleanup = new AtomicLong(0);
        volatile long earliestExpiration = 0;
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
                synchronized (window) {
                    // Double check under window lock: ensure it is still mapped to key
                    if (store.map.get(key) != window) {
                        continue;
                    }

                    long currentNow = time.getAsLong();
                    long remaining = window.expires - currentNow;
                    if (remaining <= 0) {
                        // Window expired: atomically remove old window and retry
                        if (store.map.remove(key, window)) {
                            store.earliestExpiration = 0;
                        }
                        continue;
                    }

                    if (window.count >= limit) {
                        throw new RateLimitExceeded(seconds(remaining));
                    }
                    window.count++;
                    return;
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
                        long bound = store.earliestExpiration;
                        if (bound > currentNow) {
                            throw new RateLimitExceeded(seconds(bound - currentNow));
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
        boolean removed = false;
        for (var entry : store.map.entrySet()) {
            CounterWindow w = entry.getValue();
            if (now - w.expires >= 0) {
                synchronized (w) {
                    if (now - w.expires >= 0) {
                        if (store.map.remove(entry.getKey(), w)) {
                            removed = true;
                        }
                    }
                }
            }
        }
        if (removed) {
            store.earliestExpiration = 0;
        }
    }

    private long seconds(long nanos) {
        return Math.max(1, (nanos + 999999999L) / 1000000000L);
    }

    int size() {
        return publicStore.map.size();
    }
}
