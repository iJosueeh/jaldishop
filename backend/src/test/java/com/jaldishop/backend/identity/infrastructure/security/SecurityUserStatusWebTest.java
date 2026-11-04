package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.shared.exception.RestAccessDeniedHandler;
import com.jaldishop.backend.shared.exception.RestAuthenticationEntryPoint;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        SecurityUserStatusWebTest.TestConfig.class
})
@WebAppConfiguration
class SecurityUserStatusWebTest {

    @Configuration
    @EnableWebMvc
    @Import(TestController.class)
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        public UserRepository userRepository() {
            return mock(UserRepository.class);
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/api/v1/users/me")
        public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal JwtPrincipal principal) {
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "userId", principal != null ? principal.userId().toString() : "anonymous"
            ));
        }

        @GetMapping("/api/v1/admin/users")
        public ResponseEntity<Map<String, String>> adminUsers() {
            return ResponseEntity.ok(Map.of("status", "admin_ok"));
        }

        @GetMapping("/api/v1/stores/featured")
        public ResponseEntity<Map<String, String>> getFeaturedStores() {
            return ResponseEntity.ok(Map.of("status", "featured_stores_ok"));
        }
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    private MockMvc mockMvc;
    private UUID testUserId;
    private String token;

    @BeforeEach
    void setUp() {
        reset(jwtService, userRepository);
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        testUserId = UUID.randomUUID();
        token = "test.valid.jwt.token";
    }

    private User buildUser(UUID id, UserStatus status, Set<RoleName> roleNames) {
        Set<Role> roles = roleNames.stream()
                .map(rn -> new Role((short) 1, rn))
                .collect(java.util.stream.Collectors.toSet());
        return User.reconstitute(
                id,
                "usuario@jaldishop.com",
                "encodedPassword",
                "Test",
                "User",
                "+51987654321",
                status,
                roles,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Escenario 1: JWT válido y usuario ACTIVE -> Acceso permitido (HTTP 200)")
    void whenUserIsActive_withValidJwt_shouldAllowAccess() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));
        User user = buildUser(testUserId, UserStatus.ACTIVE, Set.of(RoleName.CUSTOMER));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()));
    }

    @Test
    @DisplayName("Escenario 2: JWT válido y usuario SUSPENDED en ruta protegida -> HTTP 401 con ApiError UNAUTHORIZED")
    void whenUserIsSuspended_withValidJwt_shouldReturn401() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));
        User suspendedUser = buildUser(testUserId, UserStatus.SUSPENDED, Set.of(RoleName.CUSTOMER));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(suspendedUser));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));
    }

    @Test
    @DisplayName("Escenario 3: JWT válido y usuario INACTIVE en ruta protegida -> HTTP 401 con ApiError UNAUTHORIZED")
    void whenUserIsInactive_withValidJwt_shouldReturn401() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));
        User inactiveUser = buildUser(testUserId, UserStatus.INACTIVE, Set.of(RoleName.CUSTOMER));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(inactiveUser));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));
    }

    @Test
    @DisplayName("Escenario 4: JWT válido y usuario eliminado (no existe en BD) -> HTTP 401 con ApiError UNAUTHORIZED")
    void whenUserDeletedOrNotFound_withValidJwt_shouldReturn401() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));
    }

    @Test
    @DisplayName("Escenario 5: JWT inválido o expirado en ruta protegida -> HTTP 401 con ApiError UNAUTHORIZED")
    void whenJwtIsInvalidOrExpired_shouldReturn401() throws Exception {
        when(jwtService.getPrincipal(token)).thenThrow(new IllegalArgumentException("JWT expired or signature invalid"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Escenario 6: JWT de usuario SUSPENDED en ruta pública -> Acceso anónimo permitido (HTTP 200)")
    void whenUserSuspended_onPublicRoute_shouldAllowAnonymousAccess() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));
        User suspendedUser = buildUser(testUserId, UserStatus.SUSPENDED, Set.of(RoleName.CUSTOMER));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(suspendedUser));

        mockMvc.perform(get("/api/v1/stores/featured")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("featured_stores_ok"));
    }

    @Test
    @DisplayName("Escenario 7: JWT inválido/malformado en ruta pública -> Acceso anónimo permitido (HTTP 200)")
    void whenJwtMalformed_onPublicRoute_shouldAllowAnonymousAccess() throws Exception {
        when(jwtService.getPrincipal(token)).thenThrow(new MalformedJwtException("Token corrupto"));

        mockMvc.perform(get("/api/v1/stores/featured")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("featured_stores_ok"));

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Escenario 8: Usuario reactivado (de SUSPENDED a ACTIVE) -> El JWT anterior vuelve a ser válido")
    void whenUserReactivated_previousJwtBecomesValidAgain() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));
        User user = buildUser(testUserId, UserStatus.SUSPENDED, Set.of(RoleName.CUSTOMER));

        // 1. Mientras está suspendido: HTTP 401
        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. Se reactiva el usuario (status = ACTIVE)
        user.activate();

        // 3. Con el mismo JWT previo: ahora es HTTP 200 OK
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()));
    }

    @Test
    @DisplayName("Escenario 9: Fallo en base de datos al validar usuario en ruta protegida -> HTTP 503 SERVICE_UNAVAILABLE (no 401)")
    void whenUserRepositoryFails_onProtectedRoute_shouldReturn503ServiceUnavailable() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId))
                .thenThrow(new DataAccessResourceFailureException("PostgreSQL connection refused: connection pool exhausted"));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Servicio no disponible temporalmente"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Escenario 10: Fallo en base de datos al validar usuario en ruta pública con JWT -> HTTP 503 SERVICE_UNAVAILABLE")
    void whenUserRepositoryFails_onPublicRouteWithJwt_shouldReturn503ServiceUnavailable() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId))
                .thenThrow(new DataAccessResourceFailureException("PostgreSQL down"));

        // No asumir 200: la falla de infraestructura detiene la petición devolviendo 503
        mockMvc.perform(get("/api/v1/stores/featured")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Servicio no disponible temporalmente"));
    }

    @Test
    @DisplayName("Escenario 11: Petición anónima a ruta pública sin token -> Acceso permitido (HTTP 200)")
    void whenAnonymousRequestToPublicRoute_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/stores/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("featured_stores_ok"));

        verifyNoInteractions(jwtService);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Escenario 12: Autorización por roles -> ADMIN accede a /api/v1/admin/** (HTTP 200)")
    void whenAdminUser_accessAdminRoute_shouldSucceed() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("ADMIN"));
        User adminUser = buildUser(testUserId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(adminUser));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("admin_ok"));
    }

    @Test
    @DisplayName("Escenario 13: Autorización por roles -> CUSTOMER a /api/v1/admin/** es HTTP 403 con ApiError FORBIDDEN")
    void whenCustomerUser_accessAdminRoute_shouldReturn403Forbidden() throws Exception {
        JwtPrincipal principal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));
        User customerUser = buildUser(testUserId, UserStatus.ACTIVE, Set.of(RoleName.CUSTOMER));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(customerUser));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("No tienes permisos para realizar esta operación"));
    }
}
