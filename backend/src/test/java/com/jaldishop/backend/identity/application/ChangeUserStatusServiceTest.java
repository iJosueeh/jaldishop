package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeUserStatusServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ChangeUserStatusService changeUserStatusService;

    private User activeUser;
    private UUID userId;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        activeUser = User.reconstitute(
                userId,
                "user@jaldishop.com",
                "hashed",
                "Juan",
                "Perez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Debe suspender un usuario activo correctamente")
    void shouldSuspendActiveUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailResponse response = changeUserStatusService.suspend(userId, adminId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar auto-suspender la cuenta administradora")
    void shouldThrowWhenAdminSuspendsSelf() {
        User adminUser = User.reconstitute(
                adminId,
                "admin@jaldishop.com",
                "hashed",
                "Admin",
                "User",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> changeUserStatusService.suspend(adminId, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No puedes suspender tu propia cuenta");
    }

    @Test
    @DisplayName("Debe lanzar excepción al suspender un usuario ya suspendido")
    void shouldThrowWhenUserAlreadySuspended() {
        User suspendedUser = User.reconstitute(
                userId,
                "user@jaldishop.com",
                "hashed",
                "Juan",
                "Perez",
                "987654321",
                UserStatus.SUSPENDED,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)),
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        assertThatThrownBy(() -> changeUserStatusService.suspend(userId, adminId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra suspendido");
    }

    @Test
    @DisplayName("Debe reactivar un usuario suspendido correctamente")
    void shouldActivateSuspendedUser() {
        User suspendedUser = User.reconstitute(
                userId,
                "user@jaldishop.com",
                "hashed",
                "Juan",
                "Perez",
                "987654321",
                UserStatus.SUSPENDED,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)),
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserDetailResponse response = changeUserStatusService.activate(userId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(suspendedUser);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario a suspender no existe")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> changeUserStatusService.suspend(userId, adminId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
