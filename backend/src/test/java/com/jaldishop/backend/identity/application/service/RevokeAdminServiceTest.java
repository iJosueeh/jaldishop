package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokeAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AdminAuditService auditService;

    @InjectMocks
    private RevokeAdminService revokeAdminService;

    private UUID primaryAdminId;
    private UUID operationalAdminId;
    private UUID targetAdminId;
    private User primaryAdmin;
    private User operationalAdmin;
    private User targetAdmin;
    private Role adminRole;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        primaryAdminId = UUID.randomUUID();
        operationalAdminId = UUID.randomUUID();
        targetAdminId = UUID.randomUUID();

        customerRole = new Role((short) 1, RoleName.CUSTOMER);
        adminRole = new Role((short) 3, RoleName.ADMIN);

        primaryAdmin = User.reconstitute(
                primaryAdminId,
                "primary@jaldishop.com",
                "hashed",
                "Primary",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                true
        );

        operationalAdmin = User.reconstitute(
                operationalAdminId,
                "op@jaldishop.com",
                "hashed",
                "Op",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                false
        );

        targetAdmin = User.reconstitute(
                targetAdminId,
                "target.admin@jaldishop.com",
                "hashed",
                "Target",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                false
        );
    }

    @Test
    @DisplayName("Administrador principal revoca privilegios administrativos exitosamente e invalida sesiones")
    void shouldRevokeAdminPrivileges() {
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(targetAdminId)).thenReturn(Optional.of(targetAdmin));
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = revokeAdminService.execute(targetAdminId, primaryAdminId);

        assertThat(result.hasRole(RoleName.ADMIN)).isFalse();
        assertThat(result.hasRole(RoleName.CUSTOMER)).isTrue();
        verify(userRepository).incrementCredentialsVersion(targetAdminId);
        verify(auditService).logSuccess(eq(primaryAdminId), any(), eq("REVOKE_ADMIN"), eq("USER"), eq(targetAdminId), any(), any());
    }

    @Test
    @DisplayName("Seguridad: Administrador operativo no puede revocar administradores")
    void shouldRejectOperationalAdminFromRevoking() {
        when(userRepository.findById(operationalAdminId)).thenReturn(Optional.of(operationalAdmin));

        assertThatThrownBy(() -> revokeAdminService.execute(targetAdminId, operationalAdminId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo el administrador principal puede revocar privilegios");
    }

    @Test
    @DisplayName("Seguridad: Prohíbe revocar los privilegios del Administrador Principal")
    void shouldProtectPrimaryAdminFromRevocation() {
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));

        assertThatThrownBy(() -> revokeAdminService.execute(primaryAdminId, primaryAdminId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No se pueden revocar los privilegios del administrador principal");
    }

    @Test
    @DisplayName("Rechaza revocar a un usuario que no tiene rol ADMIN")
    void shouldRejectWhenUserNotAdmin() {
        User regularUser = User.reconstitute(
                targetAdminId,
                "regular@jaldishop.com",
                "hashed",
                "Regular",
                "User",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(customerRole),
                Instant.now(),
                Instant.now(),
                false
        );

        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(targetAdminId)).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> revokeAdminService.execute(targetAdminId, primaryAdminId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no cuenta con el rol ADMIN");
    }
}
