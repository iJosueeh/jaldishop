package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.identity.application.service.*;
import com.jaldishop.backend.identity.domain.model.*;
import com.jaldishop.backend.identity.domain.enums.*;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.web.controller.*;
import com.jaldishop.backend.shared.exception.*;
import com.jaldishop.backend.shared.security.ratelimit.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(PasswordRecoverySecurityWebTest.Config.class)
@WebAppConfiguration
class PasswordRecoverySecurityWebTest {
    @Configuration @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class, GlobalExceptionHandler.class, LoginRateLimitAdvice.class,
            RateLimitWebConfiguration.class, PasswordRecoveryController.class, AuthController.class})
    static class Config {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean JwtService jwt() { return new JwtService("local-test-signing-key-with-at-least-32-bytes", 600000); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean PasswordRecoveryService recovery() { return mock(PasswordRecoveryService.class); }
        @Bean RequestPasswordRecoveryService requests() { return mock(RequestPasswordRecoveryService.class); }
        @Bean RateLimits limits() { return new RateLimits(new MockEnvironment()); }
        @Bean AuthenticateUserService login() { return mock(AuthenticateUserService.class); }
        @Bean RegisterCustomerService register() { return mock(RegisterCustomerService.class); }
        @Bean RegisterMerchantService merchant() { return mock(RegisterMerchantService.class); }
        @Bean RefreshTokenService refresh(UserRepository users, JwtService jwt) { return new RefreshTokenService(users, jwt); }
    }
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired PasswordRecoveryService recovery;
    MockMvc mvc;
    @BeforeEach void setup() {
        reset(users, recovery);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void bothRecoveryEndpointsArePublicValidateAndNeverReturnSecrets() throws Exception {
        String response = mvc.perform(post("/api/v1/auth/forgot-password").contentType("application/json")
                .content("{\"email\":\"a@example.com\"}"))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("token", "a@example.com");
        mvc.perform(post("/api/v1/auth/reset-password").contentType("application/json")
                .content("{\"token\":\"" + "A".repeat(43) + "\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(post("/api/v1/auth/reset-password").contentType("application/json")
                .content("{\"token\":\"secret-invalid\",\"newPassword\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-invalid"))));
    }
    @Test void oldJwtCannotRefreshButNewJwtCanIncludingImmediatelyInSameSecond() throws Exception {
        User user = User.create("a@example.com", "encoded", "Test", "User", null,
                Set.of(new com.jaldishop.backend.identity.domain.model.Role((short) 1, RoleName.CUSTOMER)));
        String old = jwt.generateToken(user.getId(), Set.of("CUSTOMER"));
        user.resetPassword("new-encoded");
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + old))
                .andExpect(status().isUnauthorized());
        String current = jwt.generateToken(user.getId(), Set.of("CUSTOMER"), user.getCredentialsVersion());
        String body = mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + current))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("token");
        // Covers reset racing the time between filter authentication and refresh service lookup.
        assertThatThrownBy(() -> new RefreshTokenService(users, jwt).execute(user.getId(), 0L))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }
    @Test void legacyJwtWithoutVersionRemainsUsableUntilPasswordReset() throws Exception {
        User user = User.create("legacy@example.com", "encoded", "Test", "User", null,
                Set.of(new com.jaldishop.backend.identity.domain.model.Role((short) 1, RoleName.CUSTOMER)));
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "local-test-signing-key-with-at-least-32-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String legacy = io.jsonwebtoken.Jwts.builder().subject(user.getId().toString())
                .claim("roles", Set.of("CUSTOMER")).expiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(key).compact();
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + legacy))
                .andExpect(status().isOk());
        user.resetPassword("new-encoded");
        mvc.perform(post("/api/v1/auth/refresh").header("Authorization", "Bearer " + legacy))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + legacy))
                .andExpect(status().isUnauthorized());
    }

    @Test void ipLimitsCannotBeBypassedUsingForwardedHeaders() throws Exception {
        for (int i = 0; i < 11; i++) {
            var result = mvc.perform(post("/api/v1/auth/forgot-password").with(req -> { req.setRemoteAddr("127.0.0.10"); return req; })
                    .header("X-Forwarded-For", "10.0.0." + i).contentType("application/json")
                    .content("{\"email\":\"a" + i + "@example.com\"}"));
            if (i < 10) result.andExpect(status().isAccepted());
            else result.andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        }
    }
}
