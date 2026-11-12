package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.service.GetAdminAuditLogsService;
import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AdminAuditLogControllerSecurityTest.Config.class)
class AdminAuditLogControllerSecurityTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        GetAdminAuditLogsService getAdminAuditLogsService() {
            return mock(GetAdminAuditLogsService.class);
        }

        @Bean
        AdminAuditLogController adminAuditLogController(GetAdminAuditLogsService getAdminAuditLogsService) {
            return new AdminAuditLogController(getAdminAuditLogsService);
        }
    }

    @Autowired
    private AdminAuditLogController controller;

    @Autowired
    private GetAdminAuditLogsService getAdminAuditLogsService;

    @org.junit.jupiter.api.BeforeEach
    void resetMocks() {
        reset(getAdminAuditLogsService);
    }

    @Test
    @DisplayName("Seguridad: Petición no autenticada es rechazada")
    void unauthenticatedRequestIsRejected() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> controller.listAuditLogs(null, null, null, null, null, 0, 20))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

        verifyNoInteractions(getAdminAuditLogsService);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Seguridad: Rol CUSTOMER es rechazado")
    void customerRoleIsRejected() {
        assertThatThrownBy(() -> controller.listAuditLogs(null, null, null, null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(getAdminAuditLogsService);
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    @DisplayName("Seguridad: Rol MERCHANT es rechazado")
    void merchantRoleIsRejected() {
        assertThatThrownBy(() -> controller.listAuditLogs(null, null, null, null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(getAdminAuditLogsService);
    }

    @Test
    @DisplayName("Seguridad: Administrador operativo (sin primaryAdmin) es rechazado")
    void operationalAdminIsRejected() {
        UUID adminId = UUID.randomUUID();
        JwtPrincipal operationalPrincipal = new JwtPrincipal(adminId, Set.of("ADMIN"), 0L, false);

        var auth = new UsernamePasswordAuthenticationToken(
                operationalPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            assertThatThrownBy(() -> controller.listAuditLogs(null, null, null, null, null, 0, 20))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(getAdminAuditLogsService);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("Seguridad: Administrador principal (con primaryAdmin = true) es autorizado")
    void primaryAdminIsAuthorized() {
        UUID primaryAdminId = UUID.randomUUID();
        JwtPrincipal primaryPrincipal = new JwtPrincipal(primaryAdminId, Set.of("ADMIN"), 0L, true);

        var auth = new UsernamePasswordAuthenticationToken(
                primaryPrincipal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_PRIMARY_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            AdminAuditLog fakeLog = AdminAuditLog.record(
                    primaryAdminId,
                    "primary@jaldishop.com",
                    "USER_STATUS_UPDATE",
                    "USER",
                    UUID.randomUUID(),
                    AdminAuditStatus.SUCCESS,
                    "127.0.0.1",
                    "Details"
            );

            when(getAdminAuditLogsService.execute(isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                    .thenReturn(new PageImpl<>(List.of(fakeLog)));

            var response = controller.listAuditLogs(null, null, null, null, null, 0, 20);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().getFirst().action()).isEqualTo("USER_STATUS_UPDATE");
            verify(getAdminAuditLogsService).execute(isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
