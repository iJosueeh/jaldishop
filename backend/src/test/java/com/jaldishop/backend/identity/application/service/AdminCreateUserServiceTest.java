package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.AdminCreateUserCommand;
import com.jaldishop.backend.identity.domain.enums.RoleName;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCreateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminAuditService auditService;

    @InjectMocks
    private AdminCreateUserService adminCreateUserService;

    private Role customerRole;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        customerRole = new Role((short) 1, RoleName.CUSTOMER);
        adminId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Debe crear usuario cliente asistido desde administración con rol exclusivo CUSTOMER")
    void shouldCreateCustomerUser() {
        when(userRepository.findByEmail("cliente.nuevo@jaldishop.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminCreateUserCommand command = new AdminCreateUserCommand(
                "cliente.nuevo@jaldishop.com",
                "SecurePassword123!",
                "Nuevo",
                "Cliente",
                "987654321",
                adminId
        );

        User created = adminCreateUserService.execute(command);

        assertThat(created).isNotNull();
        assertThat(created.getEmail()).isEqualTo("cliente.nuevo@jaldishop.com");
        assertThat(created.hasRole(RoleName.CUSTOMER)).isTrue();
        assertThat(created.hasRole(RoleName.ADMIN)).isFalse();
        assertThat(created.isPrimaryAdmin()).isFalse();
        verify(auditService).logSuccess(eq(adminId), any(), eq("ADMIN_CREATE_CUSTOMER"), eq("USER"), any(), any(), any());
    }

    @Test
    @DisplayName("Rechaza crear cliente si el correo ya existe")
    void shouldRejectWhenEmailAlreadyExists() {
        when(userRepository.findByEmail("duplicado@jaldishop.com")).thenReturn(Optional.of(mock(User.class)));

        AdminCreateUserCommand command = new AdminCreateUserCommand(
                "duplicado@jaldishop.com",
                "SecurePassword123!",
                "Nuevo",
                "Cliente",
                "987654321",
                adminId
        );

        assertThatThrownBy(() -> adminCreateUserService.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("El correo ya está registrado");
    }
}
