package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(userRepository, jwtService);
    }

    @Test
    @DisplayName("execute() - Debe refrescar el token y retornar los roles actualizados del usuario")
    void executeSuccess() {
        UUID userId = UUID.randomUUID();
        User user = User.reconstitute(
                userId,
                "artesano@jaldishop.com",
                "hashed",
                "Artesano",
                "Local",
                "+51999888777",
                com.jaldishop.backend.identity.domain.UserStatus.ACTIVE,
                Set.of(new Role((short) 2, RoleName.MERCHANT)),
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(eq(userId), any())).thenReturn("new-jwt-token");

        AuthResult result = refreshTokenService.execute(userId);

        assertThat(result).isNotNull();
        assertThat(result.token()).isEqualTo("new-jwt-token");
        assertThat(result.email()).isEqualTo("artesano@jaldishop.com");
        assertThat(result.roles()).contains("MERCHANT");
    }

    @Test
    @DisplayName("execute() - Debe lanzar ResourceNotFoundException si el usuario no existe")
    void executeUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.execute(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
