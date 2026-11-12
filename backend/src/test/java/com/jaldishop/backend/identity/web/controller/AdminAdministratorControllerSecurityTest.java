package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.service.InviteAdminService;
import com.jaldishop.backend.identity.application.service.PromoteUserToAdminService;
import com.jaldishop.backend.identity.application.service.RevokeAdminInvitationService;
import com.jaldishop.backend.identity.application.service.RevokeAdminService;
import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.web.dto.InviteAdminRequest;
import com.jaldishop.backend.identity.web.mapper.AdminUserResponseMapper;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AdminAdministratorControllerSecurityTest.Config.class)
class AdminAdministratorControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean InviteAdminService inviteAdminService() { return mock(InviteAdminService.class); }
        @Bean RevokeAdminInvitationService revokeAdminInvitationService() { return mock(RevokeAdminInvitationService.class); }
        @Bean PromoteUserToAdminService promoteUserToAdminService() { return mock(PromoteUserToAdminService.class); }
        @Bean RevokeAdminService revokeAdminService() { return mock(RevokeAdminService.class); }
        @Bean com.jaldishop.backend.identity.application.service.GetAdminInvitationsService getAdminInvitationsService() {
            return mock(com.jaldishop.backend.identity.application.service.GetAdminInvitationsService.class);
        }
        @Bean AdminUserResponseMapper responseMapper() { return mock(AdminUserResponseMapper.class); }

        @Bean AdminAdministratorController adminAdministratorController(
                InviteAdminService inviteAdminService,
                RevokeAdminInvitationService revokeAdminInvitationService,
                PromoteUserToAdminService promoteUserToAdminService,
                RevokeAdminService revokeAdminService,
                com.jaldishop.backend.identity.application.service.GetAdminInvitationsService getAdminInvitationsService,
                AdminUserResponseMapper responseMapper
        ) {
            return new AdminAdministratorController(
                    inviteAdminService,
                    revokeAdminInvitationService,
                    promoteUserToAdminService,
                    revokeAdminService,
                    getAdminInvitationsService,
                    responseMapper
            );
        }
    }

    @Autowired
    private AdminAdministratorController controller;

    @Autowired
    private InviteAdminService inviteAdminService;

    @Autowired
    private com.jaldishop.backend.identity.application.service.GetAdminInvitationsService getAdminInvitationsService;

    @org.junit.jupiter.api.BeforeEach
    void resetMocks() {
        reset(inviteAdminService, getAdminInvitationsService);
    }

    @Test
    @DisplayName("Seguridad: Petición no autenticada es rechazada")
    void unauthenticatedRequestIsRejected() {
        SecurityContextHolder.clearContext();
        InviteAdminRequest request = new InviteAdminRequest("nuevo@jaldishop.com", "Nuevo", "Admin", "987654321");

        assertThatThrownBy(() -> controller.inviteAdmin(request, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(inviteAdminService);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Seguridad: Rol CUSTOMER es rechazado")
    void customerRoleIsRejected() {
        InviteAdminRequest request = new InviteAdminRequest("nuevo@jaldishop.com", "Nuevo", "Admin", "987654321");

        assertThatThrownBy(() -> controller.inviteAdmin(request, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(inviteAdminService);
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    @DisplayName("Seguridad: Rol MERCHANT es rechazado")
    void merchantRoleIsRejected() {
        InviteAdminRequest request = new InviteAdminRequest("nuevo@jaldishop.com", "Nuevo", "Admin", "987654321");

        assertThatThrownBy(() -> controller.inviteAdmin(request, null))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(inviteAdminService);
    }

    @Test
    @DisplayName("Seguridad: Administrador operativo (sin primaryAdmin) es rechazado")
    void operationalAdminIsRejected() {
        UUID adminId = UUID.randomUUID();
        JwtPrincipal operationalPrincipal = new JwtPrincipal(adminId, Set.of("ADMIN"), 0L, false); // primaryAdmin = false

        var auth = new UsernamePasswordAuthenticationToken(
                operationalPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            InviteAdminRequest request = new InviteAdminRequest("nuevo@jaldishop.com", "Nuevo", "Admin", "987654321");
            assertThatThrownBy(() -> controller.inviteAdmin(request, operationalPrincipal))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(inviteAdminService);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Seguridad: Administrador principal (con primaryAdmin = true) es autorizado")
    void primaryAdminIsAuthorized() {
        UUID primaryAdminId = UUID.randomUUID();
        JwtPrincipal primaryPrincipal = new JwtPrincipal(primaryAdminId, Set.of("ADMIN"), 0L, true); // primaryAdmin = true

        var auth = new UsernamePasswordAuthenticationToken(
                primaryPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_PRIMARY_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            AdminInvitation fakeInvitation = AdminInvitation.create(
                    "nuevo@jaldishop.com",
                    "hash-123",
                    "Nuevo",
                    "Admin",
                    "987654321",
                    primaryAdminId,
                    Instant.now().plusSeconds(3600)
            );

            when(inviteAdminService.execute(any())).thenReturn(fakeInvitation);

            InviteAdminRequest request = new InviteAdminRequest("nuevo@jaldishop.com", "Nuevo", "Admin", "987654321");
            var response = controller.inviteAdmin(request, primaryPrincipal);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().email()).isEqualTo("nuevo@jaldishop.com");
            verify(inviteAdminService).execute(any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Seguridad: Listar invitaciones rechazado para administrador operativo")
    void getInvitationsRejectedForOperationalAdmin() {
        UUID adminId = UUID.randomUUID();
        JwtPrincipal operationalPrincipal = new JwtPrincipal(adminId, Set.of("ADMIN"), 0L, false);

        var auth = new UsernamePasswordAuthenticationToken(
                operationalPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            assertThatThrownBy(() -> controller.listInvitations(null, 0, 20))
                    .isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Seguridad: Listar invitaciones autorizado para administrador principal")
    void getInvitationsAuthorizedForPrimaryAdmin() {
        UUID primaryAdminId = UUID.randomUUID();
        JwtPrincipal primaryPrincipal = new JwtPrincipal(primaryAdminId, Set.of("ADMIN"), 0L, true);

        var auth = new UsernamePasswordAuthenticationToken(
                primaryPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_PRIMARY_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            when(getAdminInvitationsService.execute(null, 0, 20))
                    .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

            var response = controller.listInvitations(null, 0, 20);
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(getAdminInvitationsService).execute(null, 0, 20);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
