package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.realtime.infrastructure.websocket.StompAuthChannelInterceptor;
import com.jaldishop.backend.realtime.infrastructure.websocket.StompDeliveryAuthorizationInterceptor;
import com.jaldishop.backend.realtime.infrastructure.websocket.WebSocketConfig;
import com.jaldishop.backend.shared.exception.RestAccessDeniedHandler;
import com.jaldishop.backend.shared.exception.RestAuthenticationEntryPoint;
import com.jaldishop.backend.shared.web.HealthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(WebCorsSecurityTest.TestCorsConfig.class)
@WebAppConfiguration
class WebCorsSecurityTest {

    @Configuration
    @EnableWebMvc
    @Import({
            SecurityConfig.class,
            WebCorsConfiguration.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class,
            HealthController.class,
            RateLimitMockController.class
    })
    static class TestCorsConfig implements WebMvcConfigurer {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
            return new JwtAuthenticationFilter(
                    org.mockito.Mockito.mock(JwtService.class),
                    org.mockito.Mockito.mock(com.jaldishop.backend.identity.domain.repository.UserRepository.class),
                    new ObjectMapper()
            );
        }
    }

    @RestController
    static class RateLimitMockController {
        @GetMapping("/api/v1/auth/register/rate-limit-check")
        public org.springframework.http.ResponseEntity<java.util.Map<String, String>> rateLimitCheck() {
            return org.springframework.http.ResponseEntity.status(429)
                    .header("Retry-After", "45")
                    .body(java.util.Map.of("code", "RATE_LIMIT_EXCEEDED"));
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://negocios-jaldishop.pages.dev",
            "https://jaldishop.net",
            "https://www.jaldishop.net",
            "https://jaldishop.pe",
            "https://www.jaldishop.pe"
    })
    @DisplayName("CORS: Orígenes exactos autorizados por defecto reciben Access-Control-Allow-Origin y Allow-Credentials")
    void whenOriginIsAllowed_shouldReturnCorsHeaders(String origin) throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("Origin", origin))
                .andExpect(status().isOk())
                .andExpect(content().string("Jaldi Shop is running"))
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.pages.dev",
            "https://attacker-app.pages.dev",
            "https://malicious.vercel.app",
            "https://random-tenant.vercel.app",
            "https://phishing-jaldishop.com",
            "http://evil-localhost.attacker.com",
            "https://store-a.jaldishop.net",
            "https://lima.jaldishop.pe",
            "https://preview-123.negocios-jaldishop.pages.dev",
            "http://localhost:3000",
            "http://localhost:4200"
    })
    @DisplayName("CORS: Orígenes no autorizados, subdominios no registrados y localhost por defecto son rechazados con 403 Forbidden")
    void whenOriginIsDisallowed_shouldNotReturnCorsHeaders(String origin) throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("Origin", origin))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS: Preflight OPTIONS para origen autorizado responde 200 con cabeceras y métodos permitidos")
    void whenValidPreflightOptions_shouldReturnSuccessAndAllowedHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "https://negocios-jaldishop.pages.dev")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://negocios-jaldishop.pages.dev"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("OPTIONS")))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Retry-After")));
    }

    @Test
    @DisplayName("CORS: Preflight OPTIONS para origen no autorizado devuelve 403 Forbidden")
    void whenInvalidPreflightOptions_shouldReturnForbidden() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "https://malicious.vercel.app")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS: Respuesta con rate limit expone cabecera Retry-After hacia el cliente")
    void whenRateLimited_shouldExposeRetryAfterHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/register/rate-limit-check")
                        .header("Origin", "https://negocios-jaldishop.pages.dev"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://negocios-jaldishop.pages.dev"))
                .andExpect(header().string("Access-Control-Expose-Headers", containsString("Retry-After")))
                .andExpect(header().string("Retry-After", "45"));
    }

    @Autowired
    private WebCorsConfiguration webCorsConfiguration;

    @Test
    @DisplayName("CORS: Inyección de @Value parsea lista limpia de 5 orígenes canónicos sin espacios ni corchetes")
    void whenDefaultConfiguration_shouldHaveCleanSeparatedOrigins() {
        List<String> origins = webCorsConfiguration.getAllowedOrigins();
        assertThat(origins).containsExactly(
                "https://negocios-jaldishop.pages.dev",
                "https://jaldishop.net",
                "https://www.jaldishop.net",
                "https://jaldishop.pe",
                "https://www.jaldishop.pe"
        );
        assertThat(origins).allSatisfy(origin -> {
            assertThat(origin).doesNotStartWith("[")
                    .doesNotEndWith("]")
                    .doesNotContain(" ")
                    .doesNotContain(",");
        });
    }

    @Test
    @DisplayName("CORS + WebSocket: Ambas configuraciones aceptan e interpretan la misma lista de orígenes")
    void whenOriginsConfigured_bothWebCorsAndWebSocketUseIdenticalList() {
        List<String> origins = webCorsConfiguration.getAllowedOrigins();
        var wsConfig = new WebSocketConfig(
                org.mockito.Mockito.mock(StompAuthChannelInterceptor.class),
                org.mockito.Mockito.mock(StompDeliveryAuthorizationInterceptor.class),
                String.join(",", origins)
        );
        assertThat(wsConfig.getAllowedOrigins()).isEqualTo(origins);
    }

    @Nested
    @SpringJUnitConfig(WebCorsSecurityTest.TestCorsConfig.class)
    @TestPropertySource(properties = {
            "app.cors.allowed-origins=http://localhost:3000,http://localhost:4200"
    })
    @WebAppConfiguration
    class LocalEnvironmentOverrideTest {
        @Autowired
        private WebApplicationContext context;
        @Autowired
        private WebCorsConfiguration localCorsConfig;
        private MockMvc localMockMvc;

        @BeforeEach
        void setUp() {
            localMockMvc = MockMvcBuilders
                    .webAppContextSetup(context)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }

        @Test
        @DisplayName("CORS Override: En desarrollo local permite localhost:3000 y localhost:4200 vía propiedad")
        void whenLocalOverrideConfigured_shouldAllowLocalhostOrigins() throws Exception {
            assertThat(localCorsConfig.getAllowedOrigins()).containsExactly(
                    "http://localhost:3000",
                    "http://localhost:4200"
            );
            localMockMvc.perform(get("/api/v1/health")
                            .header("Origin", "http://localhost:3000"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));

            localMockMvc.perform(get("/api/v1/health")
                            .header("Origin", "http://localhost:4200"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));

            localMockMvc.perform(get("/api/v1/health")
                            .header("Origin", "https://malicious.com"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }
}
