package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.ChangeUserStatusCommand;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeUserStatusServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminAuditService auditService;

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
                Instant.now(),
                false
        );
    }

    @Test
    @DisplayName("Debe suspender un usuario activo correctamente")
    void shouldSuspendActiveUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User response = changeUserStatusService.execute(new ChangeUserStatusCommand(userId, UserStatus.SUSPENDED, adminId));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(userRepository).save(activeUser);
        verify(auditService).logSuccess(eq(adminId), any(), eq("SUSPEND_USER"), eq("USER"), eq(userId), any(), any());
    }

    @Test
    @DisplayName("Seguridad: Rechaza suspender al administrador principal")
    void shouldRejectSuspendingPrimaryAdmin() {
        User primaryAdmin = User.reconstitute(
                userId,
                "primary@jaldishop.com",
                "hashed",
                "Primary",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now(),
                true
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(primaryAdmin));

        assertThatThrownBy(() -> changeUserStatusService.execute(new ChangeUserStatusCommand(userId, UserStatus.SUSPENDED, adminId)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("El administrador principal no puede ser suspendido");
    }

    @Test
    @DisplayName("Seguridad: Rechaza que un ADMIN operativo suspenda a otro ADMIN")
    void shouldRejectOperationalAdminSuspendingAnotherAdmin() {
        User targetAdmin = User.reconstitute(
                userId,
                "other.admin@jaldishop.com",
                "hashed",
                "Other",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now(),
                false
        );

        User operationalAdmin = User.reconstitute(
                adminId,
                "op.admin@jaldishop.com",
                "hashed",
                "Operational",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now(),
                false // not primary admin
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(operationalAdmin));

        assertThatThrownBy(() -> changeUserStatusService.execute(new ChangeUserStatusCommand(userId, UserStatus.SUSPENDED, adminId)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo el administrador principal puede modificar el estado de una cuenta de administrador");
    }

    @Test
    @DisplayName("Seguridad: Administrador principal sí puede suspender a un ADMIN operativo")
    void shouldAllowPrimaryAdminToSuspendOperationalAdmin() {
        User targetAdmin = User.reconstitute(
                userId,
                "other.admin@jaldishop.com",
                "hashed",
                "Other",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now(),
                false
        );

        User primaryAdmin = User.reconstitute(
                adminId,
                "primary@jaldishop.com",
                "hashed",
                "Primary",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now(),
                true // primary admin!
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(targetAdmin));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User response = changeUserStatusService.execute(new ChangeUserStatusCommand(userId, UserStatus.SUSPENDED, adminId));

        assertThat(response.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        verify(userRepository).save(targetAdmin);
    }

    @Test
    @DisplayName("Debe lanzar excepción al intentar auto-suspender la cuenta administradora")
    void shouldThrowWhenAdminSuspendsSelf() {
        User primaryAdmin = User.reconstitute(
                adminId,
                "primary@jaldishop.com",
                "hashed",
                "Primary",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now(),
                true
        );

        when(userRepository.findById(adminId)).thenReturn(Optional.of(primaryAdmin));

        assertThatThrownBy(() -> changeUserStatusService.execute(new ChangeUserStatusCommand(adminId, UserStatus.SUSPENDED, adminId)))
                .isInstanceOf(ConflictException.class);
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
                Instant.now(),
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        assertThatThrownBy(() -> changeUserStatusService.execute(new ChangeUserStatusCommand(userId, UserStatus.SUSPENDED, adminId)))
                .isInstanceOf(ConflictException.class)
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
                Instant.now(),
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User response = changeUserStatusService.execute(new ChangeUserStatusCommand(userId, UserStatus.ACTIVE, null));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(suspendedUser);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario a suspender no existe")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> changeUserStatusService.execute(new ChangeUserStatusCommand(userId, UserStatus.SUSPENDED, adminId)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
