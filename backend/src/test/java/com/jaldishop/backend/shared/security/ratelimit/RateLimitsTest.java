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

    @RestController static class Endpoints {
        @PostMapping({"/api/v1/auth/register", "/api/v1/auth/register/merchant"}) String register() { return "ok"; }
        @PostMapping("/api/v1/auth/login") String login(@RequestBody LoginRequest request) { return "ok"; }
        @GetMapping("/api/v1/stores/search") String search() { return "ok"; }
        @PostMapping("/api/v1/checkout/mercado-pago/webhook") String webhook() { return "ok"; }
    }
}
