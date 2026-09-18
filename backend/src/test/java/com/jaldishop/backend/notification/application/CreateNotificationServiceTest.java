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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private CreateNotificationService createNotificationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        createNotificationService = new CreateNotificationService(notificationRepository);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Crear notificación exitosamente")
    void createNotificationSuccessfully() {
        CreateNotificationCommand command = new CreateNotificationCommand(
                userId, NotificationType.NEW_ORDER, "Nuevo pedido", "Tienes un nuevo pedido"
        );

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = createNotificationService.execute(command);

        assertNotNull(notification);
        assertEquals(userId, notification.getUserId());
        assertEquals(NotificationType.NEW_ORDER, notification.getType());
        assertEquals("Nuevo pedido", notification.getTitle());
        assertEquals("Tienes un nuevo pedido", notification.getMessage());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Crear notificación de tipo SYSTEM")
    void createSystemNotification() {
        CreateNotificationCommand command = new CreateNotificationCommand(
                userId, NotificationType.SYSTEM, "Aviso", "Mantenimiento programado"
        );

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = createNotificationService.execute(command);

        assertEquals(NotificationType.SYSTEM, notification.getType());
        assertEquals("Aviso", notification.getTitle());
    }

    @Test
    @DisplayName("Crear notificación de tipo LOW_STOCK")
    void createLowStockNotification() {
        CreateNotificationCommand command = new CreateNotificationCommand(
                userId, NotificationType.LOW_STOCK, "Stock bajo", "Producto X por agotarse"
        );

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = createNotificationService.execute(command);

        assertEquals(NotificationType.LOW_STOCK, notification.getType());
    }
}
