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

    @RestController static class Endpoints {
        @PostMapping({"/api/v1/auth/register", "/api/v1/auth/register/merchant"}) String register() { return "ok"; }
        @PostMapping("/api/v1/auth/login") String login(@RequestBody LoginRequest request) { return "ok"; }
        @GetMapping("/api/v1/stores/search") String search() { return "ok"; }
        @PostMapping("/api/v1/checkout/mercado-pago/webhook") String webhook() { return "ok"; }
    }
}
