package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MaintenanceModeFilterTest {

    @Mock
    private PlatformSettingsProvider platformSettingsProvider;

    private ObjectMapper objectMapper;
    private MaintenanceModeFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.clearContext();
        objectMapper = new ObjectMapper();
        filter = new MaintenanceModeFilter(platformSettingsProvider, objectMapper);
    }

    @Test
    @DisplayName("Permite peticiones si maintenanceMode es false")
    void allowsRequest_whenMaintenanceModeIsFalse() throws Exception {
        PlatformSettings settings = new PlatformSettings(1, true, false, false, "Aviso", new BigDecimal("5.00"), Instant.now(), Instant.now());
        when(platformSettingsProvider.getSettings()).thenReturn(settings);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Retorna 503 Service Unavailable a usuarios públicos si maintenanceMode es true")
    void blocksPublicRequest_with503_whenMaintenanceModeIsTrue() throws Exception {
        PlatformSettings settings = new PlatformSettings(1, true, false, true, "Sistema en mantenimiento programado.", new BigDecimal("5.00"), Instant.now(), Instant.now());
        when(platformSettingsProvider.getSettings()).thenReturn(settings);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("MAINTENANCE_MODE_ACTIVE");
        assertThat(response.getContentAsString()).contains("Sistema en mantenimiento programado.");
    }

    @Test
    @DisplayName("Permite peticiones con ROLE_ADMIN incluso si maintenanceMode es true")
    void allowsAdmin_whenMaintenanceModeIsTrue() throws Exception {
        PlatformSettings settings = new PlatformSettings(1, true, false, true, "Sistema en mantenimiento.", new BigDecimal("5.00"), Instant.now(), Instant.now());
        when(platformSettingsProvider.getSettings()).thenReturn(settings);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/stores");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
