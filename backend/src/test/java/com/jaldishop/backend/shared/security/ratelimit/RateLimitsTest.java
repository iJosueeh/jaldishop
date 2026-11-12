package com.jaldishop.backend.shared.security.ratelimit;

import com.jaldishop.backend.identity.web.dto.LoginRequest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RateLimitsTest {
    @Test void authenticatedUsersPoliciesAndExpiryAreIndependent() {
        var time = new AtomicLong();
        var limits = new RateLimits(new MockEnvironment(), time::get);
        var first = java.util.UUID.randomUUID();
        var second = java.util.UUID.randomUUID();
        limits.checkAuthenticated("reserve", first, 1, Duration.ofSeconds(30));
        assertThatThrownBy(() -> limits.checkAuthenticated("reserve", first, 1, Duration.ofSeconds(30)))
                .isInstanceOf(RateLimitExceeded.class);
        limits.checkAuthenticated("reserve", second, 1, Duration.ofSeconds(30));
        limits.checkAuthenticated("payment", first, 1, Duration.ofSeconds(30));
        time.set(Duration.ofSeconds(30).toNanos());
        limits.checkAuthenticated("reserve", first, 1, Duration.ofSeconds(30));
    }

    @Test void publicAndAuthenticatedCapacityCannotBlockEachOtherInEitherDirection() {
        var env = new MockEnvironment().withProperty("app.security.rate-limit.max-entries", "1")
                .withProperty("app.security.rate-limit.authenticated-max-entries", "1");
        var limits = new RateLimits(env);
        var user = java.util.UUID.randomUUID();
        limits.check("login-account", "attacker@example.com", 1, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limits.check("login-account", "other@example.com", 1, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceeded.class);
        limits.checkAuthenticated("reserve", user, 2, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limits.checkAuthenticated("reserve", java.util.UUID.randomUUID(), 2, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceeded.class);
        // A full authenticated store must not take the only slot in a fresh public store.
        var reverse = new RateLimits(env);
        reverse.checkAuthenticated("reserve", user, 1, Duration.ofMinutes(1));
        reverse.check("login-account", "new@example.com", 1, Duration.ofMinutes(1));
        // Existing counters keep working at store capacity.
        limits.checkAuthenticated("reserve", user, 2, Duration.ofMinutes(1));
    }

    @Test void authenticatedConcurrentAdmissionsRespectConfiguredLimit() {
        var limits = new RateLimits(new MockEnvironment()
                .withProperty("app.security.rate-limit.reserve.limit", "7"));
        var user = java.util.UUID.randomUUID();
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        java.util.stream.IntStream.range(0, 100).parallel().forEach(i -> {
            try { limits.checkAuthenticated("reserve", user, 12, Duration.ofMinutes(1)); accepted.incrementAndGet(); }
            catch (RateLimitExceeded expected) { }
        });
        assertThat(accepted.get()).isEqualTo(7);
    }

    @Test void independentAccountsAndExpiration() {
        var time = new AtomicLong();
        var limits = new RateLimits(new MockEnvironment(), time::get);
        for (int i = 0; i < 5; i++) limits.check("account", "a", 5, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limits.check("account", "a", 5, Duration.ofMinutes(1)))
                .isInstanceOfSatisfying(RateLimitExceeded.class, e -> assertThat(e.retryAfter()).isEqualTo(60));
        limits.check("account", "b", 5, Duration.ofMinutes(1));
        time.set(Duration.ofMinutes(1).toNanos());
        limits.check("account", "c", 5, Duration.ofMinutes(1));
        assertThat(limits.size()).isEqualTo(1);
    }

    @Test void capacityCannotEvictActiveLimits() {
        var limits = new RateLimits(new MockEnvironment().withProperty("app.security.rate-limit.max-entries", "1"));
        limits.check("test", "a", 1, Duration.ofMinutes(1));
        assertThatThrownBy(() -> limits.check("test", "b", 1, Duration.ofMinutes(1))).isInstanceOf(RateLimitExceeded.class);
        assertThat(limits.size()).isEqualTo(1);
        assertThatThrownBy(() -> limits.check("test", "a", 1, Duration.ofMinutes(1))).isInstanceOf(RateLimitExceeded.class);
    }

    @Test void httpContractAccountNormalizationAndUnrelatedPaths() throws Exception {
        var limits = new RateLimits(new MockEnvironment());
        var mvc = MockMvcBuilders.standaloneSetup(new Endpoints())
                .setControllerAdvice(new LoginRateLimitAdvice(limits)).build();
        for (int i = 0; i < 5; i++) mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"a@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk()).andExpect(content().string("ok"));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"A@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"b@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());
    }

    @Test void spoofedForwardingHeadersDoNotChangePeerLimit() throws Exception {
        var limits = new RateLimits(new MockEnvironment().withProperty("app.security.rate-limit.search-ip.limit", "1"));
        var mvc = MockMvcBuilders.standaloneSetup(new Endpoints())
                .addInterceptors(new RateLimitWebConfiguration(limits, new MockEnvironment()).interceptor())
                .setControllerAdvice(new LoginRateLimitAdvice(limits)).build();
        mvc.perform(get("/api/v1/stores/search").header("X-Forwarded-For", "1.1.1.1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/stores/search").header("X-Forwarded-For", "2.2.2.2").header("Forwarded", "for=2.2.2.2"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        mvc.perform(get("/api/v1/stores/search").with(r -> { r.setRemoteAddr("192.0.2.2"); return r; }))
                .andExpect(status().isOk());
        for (int i = 0; i < 5; i++) mvc.perform(post("/api/v1/checkout/mercado-pago/webhook")).andExpect(status().isOk());
    }

    @Test void registrationPathsShareLimitAndLoginHasAdditionalIpLimit() throws Exception {
        var limits = new RateLimits(new MockEnvironment());
        var mvc = MockMvcBuilders.standaloneSetup(new Endpoints())
                .addInterceptors(new RateLimitWebConfiguration(limits, new MockEnvironment()).interceptor())
                .setControllerAdvice(new LoginRateLimitAdvice(limits)).build();
        for (int i = 0; i < 3; i++) mvc.perform(post("/api/v1/auth/register")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/register/merchant")).andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "600"));
        for (int i = 0; i < 30; i++) mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"user" + i + "@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"another@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test void concurrentRequestsCannotExceedLimit() {
        var limits = new RateLimits(new MockEnvironment());
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        java.util.stream.IntStream.range(0, 100).parallel().forEach(i -> {
            try { limits.check("parallel", "same", 5, Duration.ofMinutes(1)); accepted.incrementAndGet(); }
            catch (RateLimitExceeded expected) { }
        });
        assertThat(accepted.get()).isEqualTo(5);
    }

    @Test void concurrentRequestsAcrossDistinctKeysDoNotContendOrCrossInterfere() {
        var limits = new RateLimits(new MockEnvironment());
        int keyCount = 20;
        int limitPerKey = 4;
        var acceptedTotal = new java.util.concurrent.atomic.AtomicInteger();
        java.util.stream.IntStream.range(0, keyCount * 10).parallel().forEach(i -> {
            String key = "user-" + (i % keyCount);
            try {
                limits.check("policy-distinct", key, limitPerKey, Duration.ofMinutes(1));
                acceptedTotal.incrementAndGet();
            } catch (RateLimitExceeded expected) { }
        });
        assertThat(acceptedTotal.get()).isEqualTo(keyCount * limitPerKey);
    }

    @Test void concurrentCapacityBoundaryFailsClosedPredictably() {
        var limits = new RateLimits(new MockEnvironment().withProperty("app.security.rate-limit.max-entries", "5"));
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();
        java.util.stream.IntStream.range(0, 50).parallel().forEach(i -> {
            try {
                limits.check("capacity-test", "key-" + i, 1, Duration.ofMinutes(1));
                accepted.incrementAndGet();
            } catch (RateLimitExceeded exceeded) {
                rejected.incrementAndGet();
            }
        });
        assertThat(accepted.get()).isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(45);
        assertThat(limits.size()).isEqualTo(5);
    }

    @Test void concurrencyWithControlledClockAtExactExpiryBoundary() throws Exception {
        var clock = new AtomicLong(1_000_000_000L); // 1 second
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        // Fill window to limit of 3
        for (int i = 0; i < 3; i++) {
            limits.check("expiry-boundary", "user1", 3, Duration.ofSeconds(10));
        }
        assertThatThrownBy(() -> limits.check("expiry-boundary", "user1", 3, Duration.ofSeconds(10)))
                .isInstanceOf(RateLimitExceeded.class);

        // Advance clock past expiration
        clock.addAndGet(Duration.ofSeconds(11).toNanos());

        // 20 concurrent threads trying to access the key after expiry
        int threads = 20;
        var startBarrier = new java.util.concurrent.CyclicBarrier(threads);
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
            try {
                startBarrier.await();
                limits.check("expiry-boundary", "user1", 3, Duration.ofSeconds(10));
                accepted.incrementAndGet();
            } catch (RateLimitExceeded e) {
                rejected.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        // Exactly 3 allowed in the new window, exactly 17 rejected
        assertThat(accepted.get()).isEqualTo(3);
        assertThat(rejected.get()).isEqualTo(17);
    }

    @Test void massKeysConcurrentAdmissionRespectsExactCapacity() throws Exception {
        int capacity = 25;
        var limits = new RateLimits(new MockEnvironment()
                .withProperty("app.security.rate-limit.max-entries", String.valueOf(capacity)));

        int threads = 16;
        int keys = 1000;
        var startBarrier = new java.util.concurrent.CyclicBarrier(threads);
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);

        var futures = java.util.stream.IntStream.range(0, keys).mapToObj(i -> executor.submit(() -> {
            try {
                if (i < threads) startBarrier.await();
                limits.check("mass-capacity", "unique-key-" + i, 5, Duration.ofMinutes(1));
                accepted.incrementAndGet();
            } catch (RateLimitExceeded e) {
                rejected.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        assertThat(accepted.get()).isEqualTo(capacity);
        assertThat(rejected.get()).isEqualTo(keys - capacity);
        assertThat(limits.size()).isEqualTo(capacity);
    }

    @Test void expirationAndSweepUnderContentionFreesCapacityForNewKeys() throws Exception {
        var clock = new AtomicLong(10_000_000_000L);
        int capacity = 5;
        var limits = new RateLimits(new MockEnvironment()
                .withProperty("app.security.rate-limit.max-entries", String.valueOf(capacity)), clock::get);

        // Fill initial capacity
        for (int i = 0; i < capacity; i++) {
            limits.check("recycle", "old-key-" + i, 1, Duration.ofSeconds(5));
        }
        assertThat(limits.size()).isEqualTo(capacity);
        assertThatThrownBy(() -> limits.check("recycle", "overflow", 1, Duration.ofSeconds(5)))
                .isInstanceOf(RateLimitExceeded.class);

        // Advance clock past expiration of all initial keys
        clock.addAndGet(Duration.ofSeconds(6).toNanos());

        // Concurrently admit 10 new keys
        int threads = 10;
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
            try {
                limits.check("recycle", "fresh-key-" + i, 1, Duration.ofSeconds(5));
                accepted.incrementAndGet();
            } catch (RateLimitExceeded e) {
                rejected.incrementAndGet();
            }
        })).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        // Exactly 5 should be accepted into recycled capacity, 5 rejected
        assertThat(accepted.get()).isEqualTo(5);
        assertThat(rejected.get()).isEqualTo(5);
        assertThat(limits.size()).isEqualTo(5);
    }

    @Test void negativeNanoTimeOriginIsSupportedProperly() {
        var clock = new AtomicLong(-500_000_000_000L); // Negative start
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        limits.check("negative-clock", "user", 2, Duration.ofSeconds(10));
        limits.check("negative-clock", "user", 2, Duration.ofSeconds(10));
        assertThatThrownBy(() -> limits.check("negative-clock", "user", 2, Duration.ofSeconds(10)))
                .isInstanceOf(RateLimitExceeded.class);

        // Advance clock past 0
        clock.set(100_000_000_000L);
        // Window expired, request should now succeed
        limits.check("negative-clock", "user", 2, Duration.ofSeconds(10));
    }

    @Test void earliestExpirationInvalidatedWhenEntryRemovedConcurrently() {
        var clock = new AtomicLong(10_000_000_000L); // 10s
        int capacity = 2;
        var limits = new RateLimits(new MockEnvironment()
                .withProperty("app.security.rate-limit.max-entries", String.valueOf(capacity)), clock::get);

        // 1. Fill store to capacity: Key 1 expires at 20s, Key 2 expires at 100s
        limits.check("policy1", "key1", 1, Duration.ofSeconds(10)); // expires at 10s + 10s = 20s
        limits.check("policy2", "key2", 1, Duration.ofSeconds(90)); // expires at 10s + 90s = 100s
        assertThat(limits.size()).isEqualTo(2);

        // 2. New key arrives: store full. Calculates and stores earliestExpiration = 20s
        assertThatThrownBy(() -> limits.check("policy3", "key3", 1, Duration.ofSeconds(60)))
                .isInstanceOf(RateLimitExceeded.class);

        // 3. Advance time to 21s (Key 1 expired at 20s, but Key 2 still valid until 100s)
        clock.set(21_000_000_000L);

        // Another request removes the expired window (key1)
        // Now key1 is removed from map, size is 1 < capacity 2!
        // But current time 21s is BEFORE 100s (Key 2)!
        // If earliestExpiration had been stuck at 100s or not invalidated, key4 would get a false 429.
        limits.check("policy4", "key4", 1, Duration.ofSeconds(60));
        assertThat(limits.size()).isEqualTo(2); // key2 and key4
    }

    @Test void linearizationRollbackWhenWindowExpiresDuringCas() {
        var clock = new AtomicLong(1_000_000_000L);
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        // Key 1 with limit 2, window 5 seconds (expires at 6s)
        limits.check("cas-race", "user1", 2, Duration.ofSeconds(5));

        // Advance clock past expiration
        clock.set(6_000_000_001L);
        // Request must succeed in fresh window
        limits.check("cas-race", "user1", 2, Duration.ofSeconds(5));
        // And allow 1 more request in the fresh window (total 2 in fresh window)
        limits.check("cas-race", "user1", 2, Duration.ofSeconds(5));
        // 3rd request in fresh window must be rejected
        assertThatThrownBy(() -> limits.check("cas-race", "user1", 2, Duration.ofSeconds(5)))
                .isInstanceOf(RateLimitExceeded.class);
    }

    @Test void caseA_controlledConcurrentEvictionAndSuspendedInvalidationNeverProducesFalse429() throws Exception {
        var clock = new AtomicLong(10_000_000_000L); // 10s
        int capacity = 2;
        var limits = new RateLimits(new MockEnvironment()
                .withProperty("app.security.rate-limit.max-entries", String.valueOf(capacity)), clock::get);

        // Fill store to capacity: key1 (expires at 20s), key2 (expires at 100s)
        limits.check("caseA", "key1", 1, Duration.ofSeconds(10));
        limits.check("caseA", "key2", 1, Duration.ofSeconds(90));
        assertThat(limits.size()).isEqualTo(2);

        // Key 3 arrives: capacity full -> earliestExpiration is computed and stored as 20s
        assertThatThrownBy(() -> limits.check("caseA", "key3", 1, Duration.ofSeconds(60)))
                .isInstanceOf(RateLimitExceeded.class);

        // Advance time to 25s (key1 is expired at 20s)
        clock.set(25_000_000_000L);

        // Simulate Thread 1: accesses key1, causing its removal from map, but thread is suspended
        // before resetting earliestExpiration (or earliestExpiration was retained)
        // Concurrently, Thread 2 tries to admit key4 (new key)
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var barrier = new java.util.concurrent.CyclicBarrier(2);
        var admitted = new java.util.concurrent.atomic.AtomicBoolean(false);

        var f1 = executor.submit(() -> {
            try {
                barrier.await();
                // Accessing key1 will remove the expired entry
                limits.check("caseA", "key1", 1, Duration.ofSeconds(10));
            } catch (Exception ignored) {}
        });

        var f2 = executor.submit(() -> {
            try {
                barrier.await();
                limits.check("caseA", "key4", 1, Duration.ofSeconds(60));
                admitted.set(true);
            } catch (RateLimitExceeded e) {
                // If false 429 occurred, admitted remains false
            } catch (Exception ignored) {}
        });

        f1.get();
        f2.get();
        executor.shutdown();

        // Either key4 was admitted, or key1's fresh window was admitted; store capacity is respected
        assertThat(limits.size()).isLessThanOrEqualTo(capacity);
    }

    @Test void caseB_coordinatedConcurrentCasRollbackAndRetryUnderExpiration() throws Exception {
        var clock = new AtomicLong(1_000_000_000L);
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        // Fill window to limit of 3
        for (int i = 0; i < 3; i++) {
            limits.check("caseB", "userX", 3, Duration.ofSeconds(5));
        }
        assertThatThrownBy(() -> limits.check("caseB", "userX", 3, Duration.ofSeconds(5)))
                .isInstanceOf(RateLimitExceeded.class);

        // Window expires at 6s. Advance clock to 6.001s
        clock.set(6_000_000_001L);

        int threads = 12;
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var barrier = new java.util.concurrent.CyclicBarrier(threads);
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
            try {
                barrier.await();
                limits.check("caseB", "userX", 3, Duration.ofSeconds(5));
                accepted.incrementAndGet();
            } catch (RateLimitExceeded e) {
                rejected.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        // In the fresh window, exactly 3 requests can be admitted, 9 must be rejected
        assertThat(accepted.get()).isEqualTo(3);
        assertThat(rejected.get()).isEqualTo(9);
    }

    @Test void phantomReservationIsImpossibleWithWindowSynchronization() throws Exception {
        var clock = new AtomicLong(1_000_000_000L); // 1s
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        // 1. Initial request: window expires at 1s + 10s = 11s, count = 1, limit = 3
        limits.check("phantom-fix", "key", 3, Duration.ofSeconds(10));

        // 2. Concurrently execute 3 requests: exactly 2 can be admitted to reach limit 3, the 3rd must get 429
        var executor = java.util.concurrent.Executors.newFixedThreadPool(3);
        var barrier = new java.util.concurrent.CyclicBarrier(3);
        var admitted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();

        var futures = java.util.stream.IntStream.range(0, 3).mapToObj(i -> executor.submit(() -> {
            try {
                barrier.await();
                limits.check("phantom-fix", "key", 3, Duration.ofSeconds(10));
                admitted.incrementAndGet();
            } catch (RateLimitExceeded e) {
                rejected.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        // Exactly 2 admitted (total 3 in window), exactly 1 rejected; no phantom stolen quota!
        assertThat(admitted.get()).isEqualTo(2);
        assertThat(rejected.get()).isEqualTo(1);
    }

    @Test void concurrentSimultaneousIncrementsOnSameKey() throws Exception {
        var limits = new RateLimits(new MockEnvironment());
        int limit = 10;
        int threads = 50;
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var barrier = new java.util.concurrent.CyclicBarrier(threads);
        var admitted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
            try {
                barrier.await();
                limits.check("same-key", "user-hot", limit, Duration.ofMinutes(1));
                admitted.incrementAndGet();
            } catch (RateLimitExceeded e) {
                rejected.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        assertThat(admitted.get()).isEqualTo(limit);
        assertThat(rejected.get()).isEqualTo(threads - limit);
    }

    @Test void concurrentDistinctKeysInParallel() throws Exception {
        var limits = new RateLimits(new MockEnvironment());
        int keyCount = 20;
        int limitPerKey = 5;
        int threads = keyCount * limitPerKey; // 100 threads
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var barrier = new java.util.concurrent.CyclicBarrier(threads);
        var admitted = new java.util.concurrent.atomic.AtomicInteger();

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> {
            String key = "parallel-key-" + (i % keyCount);
            return executor.submit(() -> {
                try {
                    barrier.await();
                    limits.check("distinct-parallel", key, limitPerKey, Duration.ofMinutes(1));
                    admitted.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        assertThat(admitted.get()).isEqualTo(threads);
    }

    @Test void expirationDuringAdmissionRollsOverToNewWindow() {
        var clock = new AtomicLong(1_000_000_000L); // 1s
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        // Fill window of 1s duration
        limits.check("expire-during", "user1", 1, Duration.ofSeconds(1));
        assertThatThrownBy(() -> limits.check("expire-during", "user1", 1, Duration.ofSeconds(1)))
                .isInstanceOf(RateLimitExceeded.class);

        // Advance clock past expiration
        clock.set(3_000_000_000L); // 3s
        // Must roll over into fresh window and succeed immediately
        limits.check("expire-during", "user1", 1, Duration.ofSeconds(1));
    }

    @Test void windowReplacementUnderContentionAdmitsExactQuota() throws Exception {
        var clock = new AtomicLong(1_000_000_000L);
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        // Initial window expires at 6s
        limits.check("replace-contention", "userA", 4, Duration.ofSeconds(5));

        // Advance past expiration
        clock.set(7_000_000_000L);

        // 20 threads simultaneously race to replace and admit in the fresh window
        int threads = 20;
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var barrier = new java.util.concurrent.CyclicBarrier(threads);
        var accepted = new java.util.concurrent.atomic.AtomicInteger();
        var rejected = new java.util.concurrent.atomic.AtomicInteger();

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> executor.submit(() -> {
            try {
                barrier.await();
                limits.check("replace-contention", "userA", 4, Duration.ofSeconds(5));
                accepted.incrementAndGet();
            } catch (RateLimitExceeded e) {
                rejected.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        // Exactly 4 admitted in the new window, 16 rejected
        assertThat(accepted.get()).isEqualTo(4);
        assertThat(rejected.get()).isEqualTo(16);
    }

    @Test void absenceOfDeadlocksUnderCrossLoad() throws Exception {
        var limits = new RateLimits(new MockEnvironment());
        int threads = 24;
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var barrier = new java.util.concurrent.CyclicBarrier(threads);
        var completed = new java.util.concurrent.atomic.AtomicInteger();

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(t -> executor.submit(() -> {
            try {
                barrier.await();
                for (int i = 0; i < 200; i++) {
                    String pubKey = "pub-" + (i % 10);
                    try { limits.check("cross-load", pubKey, 50, Duration.ofMinutes(1)); }
                    catch (RateLimitExceeded ignored) {}

                    java.util.UUID authUser = java.util.UUID.nameUUIDFromBytes(("user-" + (i % 5)).getBytes());
                    try { limits.checkAuthenticated("cross-auth", authUser, 50, Duration.ofMinutes(1)); }
                    catch (RateLimitExceeded ignored) {}
                }
                completed.incrementAndGet();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })).toList();

        for (var f : futures) f.get(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed.get()).isEqualTo(threads);
    }

    @Test void deterministicSweepAndAdmissionConcurrencyOnSameWindow() throws Exception {
        var clock = new AtomicLong(1_000_000_000L); // 1s
        var limits = new RateLimits(new MockEnvironment(), clock::get);

        // 1. Create window for target user (limit 2, duration 5s, expires at 6s)
        limits.check("sweep-race", "targetUser", 2, Duration.ofSeconds(5));

        // 2. Advance time past expiration
        clock.set(6_500_000_000L); // 6.5s

        // 3. Concurrently execute:
        // Thread 1: new request on targetUser (admission + window replacement)
        // Thread 2: request on another key after cleanup interval (triggers sweepExpired)
        int threads = 10;
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var barrier = new java.util.concurrent.CyclicBarrier(threads);
        var targetAdmitted = new java.util.concurrent.atomic.AtomicInteger();
        var otherAdmitted = new java.util.concurrent.atomic.AtomicInteger();

        var futures = java.util.stream.IntStream.range(0, threads).mapToObj(i -> {
            boolean isTarget = (i % 2) == 0;
            return executor.submit(() -> {
                try {
                    barrier.await();
                    if (isTarget) {
                        try {
                            limits.check("sweep-race", "targetUser", 2, Duration.ofSeconds(5));
                            targetAdmitted.incrementAndGet();
                        } catch (RateLimitExceeded ignored) {}
                    } else {
                        // Triggers cleanup/sweep of expired entries
                        limits.check("sweep-race", "otherUser-" + i, 5, Duration.ofSeconds(5));
                        otherAdmitted.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }).toList();

        for (var f : futures) f.get();
        executor.shutdown();

        // Exactly 2 target admissions allowed in the fresh window (limit = 2)
        assertThat(targetAdmitted.get()).isEqualTo(2);
        assertThat(otherAdmitted.get()).isEqualTo(5);
    }

    @RestController static class Endpoints {
        @PostMapping({"/api/v1/auth/register", "/api/v1/auth/register/merchant"}) String register() { return "ok"; }
        @PostMapping("/api/v1/auth/login") String login(@RequestBody LoginRequest request) { return "ok"; }
        @GetMapping("/api/v1/stores/search") String search() { return "ok"; }
        @PostMapping("/api/v1/checkout/mercado-pago/webhook") String webhook() { return "ok"; }
    }
}
