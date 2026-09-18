package com.jaldishop.backend.notification.application;

import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.domain.NotificationRepository;
import com.jaldishop.backend.notification.domain.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUserNotificationsServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private ListUserNotificationsService listUserNotificationsService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        listUserNotificationsService = new ListUserNotificationsService(notificationRepository);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Listar notificaciones del usuario")
    void listUserNotifications() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Notification> notifications = List.of(
                Notification.create(userId, NotificationType.NEW_ORDER, "Pedido 1", "Mensaje 1"),
                Notification.create(userId, NotificationType.SYSTEM, "Aviso", "Mensaje 2")
        );

        when(notificationRepository.findByUserId(userId, pageable)).thenReturn(notifications);

        List<Notification> result = listUserNotificationsService.execute(userId, pageable);

        assertEquals(2, result.size());
        verify(notificationRepository).findByUserId(userId, pageable);
    }

    @Test
    @DisplayName("Retornar lista vacía cuando el usuario no tiene notificaciones")
    void returnEmptyListWhenNoNotifications() {
        Pageable pageable = PageRequest.of(0, 20);

        when(notificationRepository.findByUserId(userId, pageable)).thenReturn(List.of());

        List<Notification> result = listUserNotificationsService.execute(userId, pageable);

        assertTrue(result.isEmpty());
    }
}
