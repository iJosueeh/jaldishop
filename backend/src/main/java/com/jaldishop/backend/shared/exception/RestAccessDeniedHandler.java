package com.jaldishop.backend.shared.exception;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
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
import java.util.Map;
import java.util.UUID;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ObjectProvider<AdminAuditService> auditServiceProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this(objectMapper, null, null);
    }

    public RestAccessDeniedHandler(ObjectMapper objectMapper, ObjectProvider<AdminAuditService> auditServiceProvider) {
        this(objectMapper, auditServiceProvider, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RestAccessDeniedHandler(
            ObjectMapper objectMapper,
            ObjectProvider<AdminAuditService> auditServiceProvider,
            ObjectProvider<UserRepository> userRepositoryProvider
    ) {
        this.objectMapper = objectMapper;
        this.auditServiceProvider = auditServiceProvider;
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (request != null && request.getAttribute("ADMIN_AUDIT_DENIED_LOGGED") == null) {
            request.setAttribute("ADMIN_AUDIT_DENIED_LOGGED", Boolean.TRUE);
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/v1/admin") && auditServiceProvider != null) {
                auditServiceProvider.ifAvailable(auditService -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    UUID actorId = null;
                    String actorEmail = null;
                    if (auth != null && auth.getPrincipal() instanceof JwtPrincipal principal) {
                        actorId = principal.userId();
                        if (userRepositoryProvider != null && userRepositoryProvider.getIfAvailable() != null && actorId != null) {
                            actorEmail = userRepositoryProvider.getIfAvailable().findById(actorId)
                                    .map(User::getEmail)
                                    .orElse(null);
                        }
                    }
                    if (actorEmail == null && actorId != null) {
                        actorEmail = "ID:" + actorId;
                    } else if (actorEmail == null) {
                        actorEmail = "ANONYMOUS";
                    }

                    String method = request.getMethod() != null ? request.getMethod() : "UNKNOWN";
                    String reason = accessDeniedException != null && accessDeniedException.getMessage() != null
                            ? accessDeniedException.getMessage()
                            : "Acceso denegado a endpoint administrativo";

                    String detailsJson;
                    try {
                        detailsJson = objectMapper.writeValueAsString(Map.of(
                                "method", method,
                                "uri", uri,
                                "reason", reason
                        ));
                    } catch (Exception e) {
                        detailsJson = "{\"method\":\"" + method + "\",\"uri\":\"" + uri + "\"}";
                    }

                    auditService.logDenied(
                            actorId,
                            actorEmail,
                            "ACCESS_DENIED",
                            "ENDPOINT",
                            null,
                            request.getRemoteAddr(),
                            detailsJson
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
