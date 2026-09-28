package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.*;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
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
class UpdateUserRolesServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

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
                Instant.now()
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
    }

    @Test
    @DisplayName("Debe lanzar excepción si un administrador intenta removerse el rol ADMIN a sí mismo")
    void shouldThrowWhenAdminRemovesOwnAdminRole() {
        User adminUser = User.reconstitute(
                adminId,
                "admin@jaldishop.com",
                "hashed",
                "Super",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now()
        );

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> updateUserRolesService.execute(
                adminId,
                Set.of(RoleName.CUSTOMER),
                adminId
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No puedes removerte tu propio rol de administrador");
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

        assertThatThrownBy(() -> updateUserRolesService.execute(userId, Set.of(RoleName.ADMIN), adminId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
