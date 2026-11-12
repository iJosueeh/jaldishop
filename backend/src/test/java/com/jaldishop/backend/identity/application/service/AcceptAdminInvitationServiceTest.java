package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.AcceptAdminInvitationCommand;
import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.AdminInvitationTokens;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptAdminInvitationServiceTest {

    @Mock
    private AdminInvitationRepository invitationRepository;

    @Mock
    private AdminInvitationTokens tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminAuditService auditService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AcceptAdminInvitationService acceptAdminInvitationService;

    private AdminInvitation invitation;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        adminRole = new Role((short) 3, RoleName.ADMIN);

        invitation = AdminInvitation.create(
                "invited@jaldishop.com",
                "hash-123",
                "Invited",
                "User",
                "987654321",
                UUID.randomUUID(),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    @DisplayName("Debe aceptar invitación válida y crear nuevo usuario ADMIN no principal")
    void shouldAcceptValidInvitationAndCreateAdminUser() {
        when(tokenService.hash("raw-token")).thenReturn("hash-123");
        when(invitationRepository.findByTokenHashForUpdate("hash-123")).thenReturn(Optional.of(invitation));
        when(userRepository.findByEmail("invited@jaldishop.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AcceptAdminInvitationCommand command = new AcceptAdminInvitationCommand("raw-token", "StrongPassword123!");
        User user = acceptAdminInvitationService.execute(command);

        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("invited@jaldishop.com");
        assertThat(user.hasRole(RoleName.ADMIN)).isTrue();
        assertThat(user.isPrimaryAdmin()).isFalse(); // MUST be false!
        assertThat(invitation.getStatus()).isEqualTo(AdminInvitationStatus.ACCEPTED);
        verify(invitationRepository).save(invitation);
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Rechaza aceptar si la invitación ha expirado")
    void shouldRejectWhenInvitationExpired() {
        AdminInvitation expiredInvitation = AdminInvitation.create(
                "expired@jaldishop.com",
                "hash-expired",
                "Expired",
                "User",
                "987654321",
                UUID.randomUUID(),
                Instant.now().minusSeconds(10) // expired!
        );

        when(tokenService.hash("raw-expired-token")).thenReturn("hash-expired");
        when(invitationRepository.findByTokenHashForUpdate("hash-expired")).thenReturn(Optional.of(expiredInvitation));

        AcceptAdminInvitationCommand command = new AcceptAdminInvitationCommand("raw-expired-token", "StrongPassword123!");

        assertThatThrownBy(() -> acceptAdminInvitationService.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ha expirado");
    }

    @Test
    @DisplayName("Rechaza aceptar si la invitación ya fue consumida previamente")
    void shouldRejectWhenInvitationAlreadyAccepted() {
        invitation.accept(Instant.now());

        when(tokenService.hash("already-accepted-token")).thenReturn("hash-accepted");
        when(invitationRepository.findByTokenHashForUpdate("hash-accepted")).thenReturn(Optional.of(invitation));

        AcceptAdminInvitationCommand command = new AcceptAdminInvitationCommand("already-accepted-token", "StrongPassword123!");

        assertThatThrownBy(() -> acceptAdminInvitationService.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya no está pendiente");
    }

    @Test
    @DisplayName("Rechaza aceptar token que no existe")
    void shouldRejectWhenTokenNotFound() {
        when(tokenService.hash("unknown-token")).thenReturn("unknown-hash");
        when(invitationRepository.findByTokenHashForUpdate("unknown-hash")).thenReturn(Optional.empty());

        AcceptAdminInvitationCommand command = new AcceptAdminInvitationCommand("unknown-token", "StrongPassword123!");

        assertThatThrownBy(() -> acceptAdminInvitationService.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("inválido o no existe");
    }
}
