package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.InviteAdminCommand;
import com.jaldishop.backend.identity.application.port.AdminInvitationMail;
import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
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
class InviteAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminInvitationRepository invitationRepository;

    @Mock
    private AdminInvitationTokens tokenService;

    @Mock
    private AdminInvitationMail mailService;

    @Mock
    private AdminAuditService auditService;

    @InjectMocks
    private InviteAdminService inviteAdminService;

    private UUID primaryAdminId;
    private UUID operationalAdminId;
    private User primaryAdminUser;
    private User operationalAdminUser;

    @BeforeEach
    void setUp() {
        primaryAdminId = UUID.randomUUID();
        operationalAdminId = UUID.randomUUID();

        Role adminRole = new Role((short) 3, RoleName.ADMIN);

        primaryAdminUser = User.reconstitute(
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
                true // isPrimaryAdmin = true
        );

        operationalAdminUser = User.reconstitute(
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
                false // isPrimaryAdmin = false
        );
    }

    @Test
    @DisplayName("Administrador principal puede emitir invitación a nuevo administrador")
    void shouldAllowPrimaryAdminToInvite() {
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdminUser));
        when(userRepository.findByEmail("nuevo@jaldishop.com")).thenReturn(Optional.empty());
        when(invitationRepository.findPendingByEmail("nuevo@jaldishop.com")).thenReturn(Optional.empty());
        when(tokenService.generate()).thenReturn("raw-token-123");
        when(tokenService.hash("raw-token-123")).thenReturn("hash-123");
        when(invitationRepository.save(any(AdminInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InviteAdminCommand command = new InviteAdminCommand(
                "nuevo@jaldishop.com",
                "Nuevo",
                "Admin",
                "987654321",
                primaryAdminId
        );

        AdminInvitation result = inviteAdminService.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("nuevo@jaldishop.com");
        assertThat(result.getStatus()).isEqualTo(AdminInvitationStatus.PENDING);
        verify(mailService).sendInvitation(eq("nuevo@jaldishop.com"), eq("Nuevo Admin"), eq("raw-token-123"), any());
        verify(auditService).logSuccess(eq(primaryAdminId), any(), eq("INVITE_ADMIN"), eq("ADMIN_INVITATION"), any(), any(), any());
    }

    @Test
    @DisplayName("Seguridad: Rechaza que un administrador operativo invite nuevos administradores")
    void shouldRejectOperationalAdminFromInviting() {
        when(userRepository.findById(operationalAdminId)).thenReturn(Optional.of(operationalAdminUser));

        InviteAdminCommand command = new InviteAdminCommand(
                "nuevo@jaldishop.com",
                "Nuevo",
                "Admin",
                "987654321",
                operationalAdminId
        );

        assertThatThrownBy(() -> inviteAdminService.execute(command))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo el administrador principal puede invitar nuevos administradores");

        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("Rechaza invitar un correo que ya pertenece a un usuario existente")
    void shouldRejectWhenEmailAlreadyExistsAsUser() {
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdminUser));
        when(userRepository.findByEmail("existente@jaldishop.com")).thenReturn(Optional.of(mock(User.class)));

        InviteAdminCommand command = new InviteAdminCommand(
                "existente@jaldishop.com",
                "Nombre",
                "Apellido",
                "987654321",
                primaryAdminId
        );

        assertThatThrownBy(() -> inviteAdminService.execute(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ya existe un usuario registrado con este correo");
    }

    @Test
    @DisplayName("Re-invitación: Si existe una invitación PENDING previa, la revoca antes de crear la nueva")
    void shouldRevokeExistingPendingInvitationOnReinvite() {
        AdminInvitation oldPending = AdminInvitation.create(
                "reinvite@jaldishop.com",
                "old-hash",
                "Re",
                "Invite",
                "987654321",
                primaryAdminId,
                Instant.now().plusSeconds(3600)
        );

        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdminUser));
        when(userRepository.findByEmail("reinvite@jaldishop.com")).thenReturn(Optional.empty());
        when(invitationRepository.findPendingByEmail("reinvite@jaldishop.com")).thenReturn(Optional.of(oldPending));
        when(tokenService.generate()).thenReturn("new-raw-token");
        when(tokenService.hash("new-raw-token")).thenReturn("new-hash");
        when(invitationRepository.save(any(AdminInvitation.class))).thenAnswer(inv -> inv.getArgument(0));

        InviteAdminCommand command = new InviteAdminCommand(
                "reinvite@jaldishop.com",
                "Re",
                "Invite",
                "987654321",
                primaryAdminId
        );

        AdminInvitation result = inviteAdminService.execute(command);

        assertThat(oldPending.getStatus()).isEqualTo(AdminInvitationStatus.REVOKED);
        assertThat(result.getStatus()).isEqualTo(AdminInvitationStatus.PENDING);
        verify(invitationRepository, atLeast(2)).save(any(AdminInvitation.class));
    }
}
