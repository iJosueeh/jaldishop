package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
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
class UpdateUserRolesServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AdminAuditService auditService;

    @InjectMocks
    private UpdateUserRolesService updateUserRolesService;

    private User customerUser;
    private UUID userId;
    private UUID adminId;
    private Role customerRole;
    private Role merchantRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        customerRole = new Role((short) 1, RoleName.CUSTOMER);
        merchantRole = new Role((short) 2, RoleName.MERCHANT);
        adminRole = new Role((short) 3, RoleName.ADMIN);

        customerUser = User.reconstitute(
                userId,
                "user@jaldishop.com",
                "hashed",
                "Juan",
                "Perez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(customerRole),
                Instant.now(),
                Instant.now(),
                false
        );
    }

    @Test
    @DisplayName("Debe actualizar roles de usuario correctamente a MERCHANT")
    void shouldUpdateUserRolesToMerchant() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(customerUser));
        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User response = updateUserRolesService.execute(
                userId,
                Set.of(RoleName.MERCHANT),
                adminId
        );

        assertThat(response).isNotNull();
        assertThat(response.hasRole(RoleName.MERCHANT)).isTrue();
        assertThat(response.hasRole(RoleName.CUSTOMER)).isFalse();
        verify(userRepository).save(customerUser);
        verify(auditService).logSuccess(eq(adminId), any(), eq("UPDATE_USER_ROLES"), eq("USER"), eq(userId), any(), any());
    }

    @Test
    @DisplayName("Seguridad: Rechaza asignar rol ADMIN mediante actualización genérica de roles")
    void shouldRejectAddingAdminRoleViaGenericUpdate() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> updateUserRolesService.execute(
                userId,
                Set.of(RoleName.ADMIN, RoleName.CUSTOMER),
                adminId
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No se puede asignar el rol ADMIN mediante actualización genérica");
    }

    @Test
    @DisplayName("Seguridad: Rechaza revocar rol ADMIN a un administrador mediante actualización genérica")
    void shouldRejectRevokingAdminRoleViaGenericUpdate() {
        User adminUser = User.reconstitute(
                userId,
                "admin@jaldishop.com",
                "hashed",
                "Admin",
                "User",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> updateUserRolesService.execute(
                userId,
                Set.of(RoleName.CUSTOMER),
                adminId
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No se puede revocar el rol ADMIN mediante actualización genérica");
    }

    @Test
    @DisplayName("Seguridad: Rechaza modificar roles de un administrador mediante actualización genérica")
    void shouldRejectModifyingAdminRolesViaGenericUpdate() {
        User adminUser = User.reconstitute(
                userId,
                "admin@jaldishop.com",
                "hashed",
                "Admin",
                "User",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> updateUserRolesService.execute(
                userId,
                Set.of(RoleName.ADMIN, RoleName.CUSTOMER),
                adminId
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No se pueden modificar los roles de un administrador mediante este endpoint");
    }

    @Test
    @DisplayName("Seguridad: Rechaza modificar roles del administrador principal bajo cualquier circunstancia")
    void shouldRejectModifyingPrimaryAdminRoles() {
        User primaryAdmin = User.reconstitute(
                userId,
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

        when(userRepository.findById(userId)).thenReturn(Optional.of(primaryAdmin));

        assertThatThrownBy(() -> updateUserRolesService.execute(
                userId,
                Set.of(RoleName.ADMIN),
                adminId
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No se pueden modificar los roles del administrador principal");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el conjunto de roles es nulo o vacío")
    void shouldThrowWhenRolesEmpty() {
        assertThatThrownBy(() -> updateUserRolesService.execute(userId, Set.of(), adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos un rol");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateUserRolesService.execute(userId, Set.of(RoleName.CUSTOMER), adminId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
