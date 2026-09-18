package com.jaldishop.backend.notification.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.notification.application.CountUnreadNotificationsService;
import com.jaldishop.backend.notification.application.ListUserNotificationsService;
import com.jaldishop.backend.notification.application.MarkNotificationAsReadService;
import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.domain.NotificationType;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private ListUserNotificationsService listUserNotificationsService;

    @Mock
    private CountUnreadNotificationsService countUnreadNotificationsService;

    @Mock
    private MarkNotificationAsReadService markNotificationAsReadService;

    private MockMvc mockMvc;
    private UUID testUserId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        NotificationController controller = new NotificationController(
                listUserNotificationsService, countUnreadNotificationsService, markNotificationAsReadService);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(JwtPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/notifications - Listar notificaciones (200 OK)")
    void listNotificationsSuccess() throws Exception {
        List<Notification> notifications = List.of(
                Notification.create(testUserId, NotificationType.NEW_ORDER, "Pedido 1", "Mensaje 1"),
                Notification.create(testUserId, NotificationType.SYSTEM, "Aviso", "Mensaje 2")
        );

        when(listUserNotificationsService.execute(eq(testUserId), any())).thenReturn(notifications);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("NEW_ORDER"))
                .andExpect(jsonPath("$[0].title").value("Pedido 1"))
                .andExpect(jsonPath("$[1].type").value("SYSTEM"))
                .andExpect(jsonPath("$[1].title").value("Aviso"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications - Retornar lista vacía cuando no hay notificaciones")
    void listNotificationsEmpty() throws Exception {
        when(listUserNotificationsService.execute(eq(testUserId), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread/count - Contar no leídas (200 OK)")
    void countUnreadSuccess() throws Exception {
        when(countUnreadNotificationsService.execute(testUserId)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read - Marcar como leída (200 OK)")
    void markAsReadSuccess() throws Exception {
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.create(
                testUserId, NotificationType.NEW_ORDER, "Pedido", "Mensaje"
        );
        notification.markAsRead();

        when(markNotificationAsReadService.execute(notificationId, testUserId)).thenReturn(notification);

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").isNotEmpty());
    }

    @Test
    @DisplayName("PATCH /api/v1/notifications/{id}/read - 404 cuando la notificación no existe")
    void markAsReadNotFound() throws Exception {
        UUID notificationId = UUID.randomUUID();

        when(markNotificationAsReadService.execute(notificationId, testUserId))
                .thenThrow(new ResourceNotFoundException("Notification", notificationId));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}
