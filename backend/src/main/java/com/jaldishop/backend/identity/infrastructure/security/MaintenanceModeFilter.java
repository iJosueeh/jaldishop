package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.shared.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final PlatformSettingsProvider platformSettingsProvider;
    private final ObjectMapper objectMapper;

    public MaintenanceModeFilter(PlatformSettingsProvider platformSettingsProvider, ObjectMapper objectMapper) {
        this.platformSettingsProvider = platformSettingsProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        PlatformSettings settings = platformSettingsProvider.getSettings();

        if (settings != null && settings.maintenanceMode()) {
            String path = request.getRequestURI();

            // Endpoints siempre exentos de mantenimiento
            if (isExemptPath(path)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Administradores autenticados siempre pueden operar
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() &&
                    auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
                filterChain.doFilter(request, response);
                return;
            }

            // Bloquear tráfico general con 503 SERVICE_UNAVAILABLE
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            String notice = (settings.maintenanceNotice() != null && !settings.maintenanceNotice().isBlank())
                    ? settings.maintenanceNotice()
                    : "La plataforma se encuentra en mantenimiento programado. Volveremos pronto.";

            ApiError error = new ApiError("MAINTENANCE_MODE_ACTIVE", notice);
            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        return path.startsWith("/api/v1/health") ||
                path.startsWith("/actuator/health") ||
                path.startsWith("/api/v1/admin/") ||
                path.startsWith("/api/v1/auth/login");
    }
}
