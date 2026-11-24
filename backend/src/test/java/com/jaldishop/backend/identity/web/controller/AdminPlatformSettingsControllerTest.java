package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.command.UpdatePlatformSettingsCommand;
import com.jaldishop.backend.identity.application.service.PlatformSettingsService;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.identity.web.dto.PlatformSettingsResponse;
import com.jaldishop.backend.identity.web.dto.UpdatePlatformSettingsRequest;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AdminPlatformSettingsControllerTest.Config.class)
class AdminPlatformSettingsControllerTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean
        PlatformSettingsService platformSettingsService() {
            return mock(PlatformSettingsService.class);
        }

        @Bean
        AdminPlatformSettingsController adminPlatformSettingsController(PlatformSettingsService platformSettingsService) {
            return new AdminPlatformSettingsController(platformSettingsService);
        }
    }

    @Autowired
    private AdminPlatformSettingsController controller;

    @Autowired
    private PlatformSettingsService platformSettingsService;

    @BeforeEach
    void resetContext() {
        reset(platformSettingsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings: lanza AuthenticationCredentialsNotFoundException sin autenticación")
    void getSettings_unauthenticated_throwsException() {
        assertThatThrownBy(() -> controller.getSettings())
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings: 403 AccessDeniedException cuando es ADMIN pero no es Primary Admin")
    void getSettings_regularAdmin_accessDenied() {
        UUID adminId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(adminId, Set.of("ROLE_ADMIN"), 0L, false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        assertThatThrownBy(() -> controller.getSettings())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings: éxito cuando es Primary Admin")
    void getSettings_primaryAdmin_success() {
        UUID adminId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(adminId, Set.of("ROLE_ADMIN"), 0L, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        PlatformSettings settings = new PlatformSettings(
                1,
                true,
                false,
                false,
                "Aviso",
                new BigDecimal("5.00"),
                0L,
                Instant.now(),
                Instant.now()
        );
        when(platformSettingsService.getSettings()).thenReturn(settings);

        ResponseEntity<PlatformSettingsResponse> response = controller.getSettings();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().allowMerchantRegistration()).isTrue();
        assertThat(response.getBody().requireStoreApproval()).isFalse();
        assertThat(response.getBody().marketplaceCommissionRate()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("PUT /api/v1/admin/settings: actualiza exitosamente con Primary Admin")
    void updateSettings_primaryAdmin_success() {
        UUID adminId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(adminId, Set.of("ROLE_ADMIN"), 0L, true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        UpdatePlatformSettingsRequest request = new UpdatePlatformSettingsRequest(
                false,
                true,
                true,
                "Mantenimiento en curso",
                new BigDecimal("7.00"),
                0L
        );

        PlatformSettings updated = new PlatformSettings(
                1,
                false,
                true,
                true,
                "Mantenimiento en curso",
                new BigDecimal("7.00"),
                1L,
                Instant.now(),
                Instant.now()
        );

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("192.168.1.100");
        when(platformSettingsService.updateSettings(any(UpdatePlatformSettingsCommand.class), eq(adminId), any(), eq("192.168.1.100")))
                .thenReturn(updated);

        ResponseEntity<PlatformSettingsResponse> response = controller.updateSettings(request, principal, servletRequest);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().allowMerchantRegistration()).isFalse();
        assertThat(response.getBody().requireStoreApproval()).isTrue();
        assertThat(response.getBody().maintenanceMode()).isTrue();
        assertThat(response.getBody().maintenanceNotice()).isEqualTo("Mantenimiento en curso");
    }
}
