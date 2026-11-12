package com.jaldishop.backend.shared.exception;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ObjectProvider<AdminAuditService> auditServiceProvider;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RestAccessDeniedHandler(ObjectMapper objectMapper, ObjectProvider<AdminAuditService> auditServiceProvider) {
        this.objectMapper = objectMapper;
        this.auditServiceProvider = auditServiceProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (request != null && request.getAttribute("ADMIN_AUDIT_DENIED_LOGGED") == null) {
            request.setAttribute("ADMIN_AUDIT_DENIED_LOGGED", Boolean.TRUE);
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/v1/admin")) {
                auditServiceProvider.ifAvailable(auditService -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    UUID actorId = null;
                    if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
                        actorId = principal.userId();
                    }
                    auditService.logDenied(
                            actorId,
                            null,
                            "ACCESS_DENIED",
                            "ADMIN_ROUTE",
                            null,
                            request.getRemoteAddr(),
                            "Acceso denegado a ruta: " + uri
                    );
                });
            }
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
                response.getOutputStream(),
                new ApiError("FORBIDDEN", "No tienes permisos para realizar esta operación")
        );
    }
}
