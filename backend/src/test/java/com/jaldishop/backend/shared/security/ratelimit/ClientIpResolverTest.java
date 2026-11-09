package com.jaldishop.backend.shared.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ClientIpResolverTest {
    private MockEnvironment render() {
        return new MockEnvironment().withProperty("RENDER", "true")
                .withProperty("RENDER_EXTERNAL_HOSTNAME", "jaldishop-api.onrender.com")
                .withProperty("app.security.rate-limit.render-ip-verified", "true");
    }

    @Test void renderIpModeIsPendingByDefaultAndHeadersCannotEnableIt() throws Exception {
        var env = new MockEnvironment().withProperty("RENDER", "true")
                .withProperty("RENDER_EXTERNAL_HOSTNAME", "jaldishop-api.onrender.com");
        var resolver = new ClientIpResolver(env);
        assertThat(resolver.enabled()).isFalse();
        var limits = new RateLimits(env);
        var mvc = MockMvcBuilders.standaloneSetup(new RateLimitsTest.Endpoints())
                .addInterceptors(new RateLimitWebConfiguration(limits, env).interceptor())
                .setControllerAdvice(new LoginRateLimitAdvice(limits)).build();
        mvc.perform(get("/api/v1/stores/search")).andExpect(status().isOk());
        for (int i = 0; i < 61; i++) mvc.perform(get("/api/v1/stores/search")
                .header("RATE_LIMIT_RENDER_IP_VERIFIED", "true").header("CF-Connecting-IP", "192.0.2.1"))
                .andExpect(status().isOk());
        for (int i = 0; i < 4; i++) mvc.perform(post("/api/v1/auth/register"))
                .andExpect(status().isOk());
        assertThat(limits.size()).isZero();
        for (int i = 0; i < 5; i++) mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"a@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"a@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test void localIpPoliciesRemainEnabledAndUnreviewedRenderHostCannotOptIn() {
        assertThat(new ClientIpResolver(new MockEnvironment()).enabled()).isTrue();
        assertThatThrownBy(() -> new ClientIpResolver(render().withProperty("RENDER_EXTERNAL_HOSTNAME", "other.onrender.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void directConnectionsIgnoreAllIpHeadersAndHost() {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("CF-Connecting-IP", "198.51.100.1");
        request.addHeader("X-Forwarded-For", "198.51.100.2");
        request.addHeader("Host", "jaldishop-api.onrender.com");
        request.addHeader("RENDER", "true");
        assertThat(new ClientIpResolver(new MockEnvironment()).resolve(request)).isEqualTo("192.0.2.10");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "example.com", "127.1", "001.2.3.4", "256.1.1.1", "1.2.3.4, 5.6.7.8", "[::1]", "fe80::1%eth0", "1.2.3.4:80"})
    void renderRejectsMalformedValuesWithoutProxyFallback(String value) {
        var request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", value);
        assertThatThrownBy(() -> new ClientIpResolver(render()).resolve(request))
                .isInstanceOf(ClientIpResolver.Unavailable.class);
    }

    @Test void rejectsMissingAndDuplicateValuesAndCanonicalizesIpv6() {
        var request = new MockHttpServletRequest();
        var resolver = new ClientIpResolver(render());
        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(ClientIpResolver.Unavailable.class);
        request.addHeader("CF-Connecting-IP", "2001:db8::1");
        String first = resolver.resolve(request);
        request.removeHeader("CF-Connecting-IP");
        request.addHeader("CF-Connecting-IP", "2001:0db8:0:0:0:0:0:1");
        assertThat(resolver.resolve(request)).isEqualTo(first);
        request.addHeader("CF-Connecting-IP", "192.0.2.1");
        assertThatThrownBy(() -> resolver.resolve(request)).isInstanceOf(ClientIpResolver.Unavailable.class);
    }

    @Test void optedInParserSeparatesCountersAndIgnoresXffWithoutProvingEdgeAuthenticity() throws Exception {
        var env = render().withProperty("app.security.rate-limit.search-ip.limit", "1");
        var limits = new RateLimits(env);
        var mvc = MockMvcBuilders.standaloneSetup(new RateLimitsTest.Endpoints())
                .addInterceptors(new RateLimitWebConfiguration(limits, env).interceptor())
                .setControllerAdvice(new LoginRateLimitAdvice(limits)).build();
        mvc.perform(get("/api/v1/stores/search").header("CF-Connecting-IP", "192.0.2.1"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/stores/search").header("CF-Connecting-IP", "192.0.2.2"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/stores/search").header("CF-Connecting-IP", "192.0.2.1")
                .header("X-Forwarded-For", "8.8.8.8").header("Forwarded", "for=8.8.8.8")
                .header("True-Client-IP", "8.8.8.8").header("X-Real-IP", "8.8.8.8"))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        mvc.perform(get("/api/v1/stores/search")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CLIENT_IP_UNAVAILABLE"));
        for (String path : new String[]{"/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/register/merchant"})
            mvc.perform(post(path)).andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("CLIENT_IP_UNAVAILABLE"));
        mvc.perform(post("/api/v1/checkout/mercado-pago/webhook")).andExpect(status().isOk());
    }
}
