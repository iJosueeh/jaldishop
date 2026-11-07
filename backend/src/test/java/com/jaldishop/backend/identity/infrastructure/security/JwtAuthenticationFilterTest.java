package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.application.service.RefreshTokenService;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.CannotCreateTransactionException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private UUID userId;
    private String token;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        userId = UUID.randomUUID();
        token = "valid.jwt.token";
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(UUID id, UserStatus status, Set<RoleName> roleNames) {
        Set<Role> roles = roleNames.stream()
                .map(rn -> new Role((short) 1, rn))
                .collect(java.util.stream.Collectors.toSet());
        return User.reconstitute(
                id,
                "user@example.com",
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
    void signedTokenAndRefreshUseCurrentRolesAfterRevocation() throws Exception {
        JwtService realJwt = new JwtService("test-only-signing-key-at-least-32-bytes-long", 600_000);
        String oldToken = realJwt.generateToken(userId, Set.of("ADMIN", "CUSTOMER"));
        User customer = buildUser(userId, UserStatus.ACTIVE, Set.of(RoleName.CUSTOMER));
        when(userRepository.findById(userId)).thenReturn(Optional.of(customer));
        var realFilter = new JwtAuthenticationFilter(realJwt, userRepository, objectMapper);
        request.addHeader("Authorization", "Bearer " + oldToken);

        realFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(((JwtPrincipal) authentication.getPrincipal()).roles()).containsExactly("CUSTOMER");
        assertThat(authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ROLE_CUSTOMER");
        String refreshed = new RefreshTokenService(userRepository, realJwt).execute(userId).token();
        assertThat(realJwt.getPrincipal(refreshed).roles()).containsExactly("CUSTOMER");
        assertThat(realJwt.getPrincipal(oldToken).roles()).contains("ADMIN");
    }

    @Test
    @DisplayName("HAL-02: Usuario ACTIVE con JWT válido es autenticado correctamente en SecurityContext")
    void whenUserIsActive_withValidJwt_shouldAuthenticateAndSetAuthorities() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER", "MERCHANT"));
        User activeUser = buildUser(userId, UserStatus.ACTIVE, Set.of(RoleName.CUSTOMER, RoleName.MERCHANT));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isEqualTo(principal);
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_MERCHANT");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: Usuario SUSPENDED con JWT previo es rechazado (SecurityContext queda vacío)")
    void whenUserIsSuspended_withValidJwt_shouldNotAuthenticateAndClearContext() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        User suspendedUser = buildUser(userId, UserStatus.SUSPENDED, Set.of(RoleName.CUSTOMER));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: Usuario INACTIVE con JWT previo es rechazado (SecurityContext queda vacío)")
    void whenUserIsInactive_withValidJwt_shouldNotAuthenticateAndClearContext() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        User inactiveUser = buildUser(userId, UserStatus.INACTIVE, Set.of(RoleName.CUSTOMER));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(inactiveUser));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: Usuario eliminado/inexistente con JWT previo es rechazado")
    void whenUserDoesNotExist_withValidJwt_shouldNotAuthenticateAndClearContext() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: JWT inválido o expirado no autentica, limpia contexto y continúa filterChain")
    void whenJwtIsInvalidOrExpired_shouldClearContextAndContinueChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.getPrincipal(token)).thenThrow(new IllegalArgumentException("Token expirado o firma corrupta"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: JwtException (token malformado) limpia contexto y continúa filterChain sin consultar BD")
    void whenJwtMalformed_shouldClearContextAndContinueChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.getPrincipal(token)).thenThrow(new MalformedJwtException("Token malformado"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: Fallo en base de datos (DataAccessException) responde 503 SERVICE_UNAVAILABLE y detiene la cadena")
    void whenDatabaseFailsWithDataAccessException_shouldReturn503AndHaltChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId))
                .thenThrow(new DataAccessResourceFailureException("PostgreSQL connection refused: connection pool exhausted"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("SERVICE_UNAVAILABLE");
        assertThat(response.getContentAsString()).contains("Servicio no disponible temporalmente");
    }

    @Test
    @DisplayName("HAL-02: Fallo en transacción (CannotCreateTransactionException) responde 503 SERVICE_UNAVAILABLE y detiene la cadena")
    void whenTransactionFails_shouldReturn503AndHaltChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId))
                .thenThrow(new CannotCreateTransactionException("Could not open JDBC Connection for transaction"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("HAL-02: Fallo JPA (PersistenceException) responde 503 SERVICE_UNAVAILABLE y detiene la cadena")
    void whenPersistenceException_shouldReturn503AndHaltChain() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId))
                .thenThrow(new PersistenceException("EntityManager is closed"));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verifyNoInteractions(filterChain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains("SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("HAL-02: Excepciones de programación no son capturadas indiscriminadamente y se propagan")
    void whenProgrammingErrorOccurs_shouldNotCatchIndiscriminately() {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId)).thenThrow(new NullPointerException("Bug in mapper or code"));

        assertThatThrownBy(() -> filter.doFilter(request, response, filterChain))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("HAL-02: Usuario reactivado (de SUSPENDED a ACTIVE) vuelve a ser autenticado")
    void whenUserIsReactivated_shouldAuthenticateSuccessfully() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));
        User user = buildUser(userId, UserStatus.SUSPENDED, Set.of(RoleName.CUSTOMER));
        user.activate(); // Transición de estado a ACTIVE

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: Petición sin cabecera Authorization pasa sin modificar contexto ni consultar BD")
    void whenNoAuthorizationHeader_shouldPassThroughWithoutModifyingContext() throws ServletException, IOException {
        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: Petición con cabecera Authorization no-Bearer pasa sin modificar contexto")
    void whenAuthorizationHeaderNotBearer_shouldPassThroughWithoutModifyingContext() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();
        verifyNoInteractions(jwtService);
        verifyNoInteractions(userRepository);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("HAL-02: Autorización por roles asigna prefijos ROLE_ a todas las autoridades")
    void roleBasedAuthorities_shouldMapAllRolesWithPrefix() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer " + token);
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("ADMIN", "MERCHANT"));
        User adminUser = buildUser(userId, UserStatus.ACTIVE, Set.of(RoleName.ADMIN, RoleName.MERCHANT));

        when(jwtService.getPrincipal(token)).thenReturn(principal);
        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_MERCHANT");
    }
}
