package com.jaldishop.backend.notification.application;

import com.jaldishop.backend.notification.domain.Notification;
import com.jaldishop.backend.notification.domain.NotificationRepository;
import com.jaldishop.backend.notification.domain.NotificationStatus;
import com.jaldishop.backend.notification.domain.NotificationType;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkNotificationAsReadServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private MarkNotificationAsReadService markNotificationAsReadService;

    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        markNotificationAsReadService = new MarkNotificationAsReadService(notificationRepository);
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Marcar notificación como leída exitosamente")
    void markAsReadSuccessfully() {
        Notification notification = Notification.create(
                userId, NotificationType.NEW_ORDER, "Pedido", "Mensaje"
        );

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = markNotificationAsReadService.execute(notificationId, userId);

        assertEquals(NotificationStatus.READ, result.getStatus());
        assertNotNull(result.getReadAt());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Lanzar excepción cuando la notificación no existe")
    void throwExceptionWhenNotificationNotFound() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> markNotificationAsReadService.execute(notificationId, userId)
        );
    }

    @Test
    @DisplayName("Lanzar excepción cuando la notificación no pertenece al usuario")
    void throwExceptionWhenNotificationDoesNotBelongToUser() {
        UUID otherUserId = UUID.randomUUID();
        Notification notification = Notification.create(
                otherUserId, NotificationType.NEW_ORDER, "Pedido", "Mensaje"
        );

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(
                ResourceNotFoundException.class,
                () -> markNotificationAsReadService.execute(notificationId, userId)
        );
    }
}
