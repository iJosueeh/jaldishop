package com.jaldishop.backend.notification.domain;

import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    @DisplayName("Crear notificación con valores válidos")
    void createNotificationWithValidValues() {
        Notification notification = Notification.create(
                userId,
                NotificationType.NEW_ORDER,
                "Nuevo pedido",
                "Tienes un nuevo pedido #1234"
        );

        assertNotNull(notification.getId());
        assertEquals(userId, notification.getUserId());
        assertEquals(NotificationType.NEW_ORDER, notification.getType());
        assertEquals("Nuevo pedido", notification.getTitle());
        assertEquals("Tienes un nuevo pedido #1234", notification.getMessage());
        assertEquals(NotificationStatus.UNREAD, notification.getStatus());
        assertNotNull(notification.getCreatedAt());
        assertNull(notification.getReadAt());
        assertTrue(notification.isUnread());
    }

    @Test
    @DisplayName("Crear notificación con todos los tipos válidos")
    void createNotificationWithAllTypes() {
        for (NotificationType type : NotificationType.values()) {
            Notification notification = Notification.create(userId, type, "Título", "Mensaje");
            assertEquals(type, notification.getType());
        }
    }

    @Test
    @DisplayName("Lanzar excepción cuando userId es nulo")
    void throwExceptionWhenUserIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Notification.create(null, NotificationType.SYSTEM, "Título", "Mensaje")
        );
        assertTrue(exception.getMessage().contains("userId"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando type es nulo")
    void throwExceptionWhenTypeIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Notification.create(userId, null, "Título", "Mensaje")
        );
        assertTrue(exception.getMessage().contains("type"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando title es nulo")
    void throwExceptionWhenTitleIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Notification.create(userId, NotificationType.SYSTEM, null, "Mensaje")
        );
        assertTrue(exception.getMessage().contains("title"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando title está en blanco")
    void throwExceptionWhenTitleIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Notification.create(userId, NotificationType.SYSTEM, "   ", "Mensaje")
        );
        assertTrue(exception.getMessage().contains("title"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando message es nulo")
    void throwExceptionWhenMessageIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Notification.create(userId, NotificationType.SYSTEM, "Título", null)
        );
        assertTrue(exception.getMessage().contains("message"));
    }

    @Test
    @DisplayName("Lanzar excepción cuando message está en blanco")
    void throwExceptionWhenMessageIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Notification.create(userId, NotificationType.SYSTEM, "Título", "  ")
        );
        assertTrue(exception.getMessage().contains("message"));
    }

    @Test
    @DisplayName("markAsRead() debe cambiar status a READ y setear readAt")
    void markAsReadShouldChangeStatusAndSetReadAt() {
        Notification notification = Notification.create(
                userId, NotificationType.SYSTEM, "Título", "Mensaje"
        );

        assertEquals(NotificationStatus.UNREAD, notification.getStatus());
        assertNull(notification.getReadAt());

        notification.markAsRead();

        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertNotNull(notification.getReadAt());
        assertFalse(notification.isUnread());
    }

    @Test
    @DisplayName("markAsRead() debe lanzar excepción si ya está marcada como leída")
    void markAsReadShouldThrowExceptionIfAlreadyRead() {
        Notification notification = Notification.create(
                userId, NotificationType.SYSTEM, "Título", "Mensaje"
        );

        notification.markAsRead();

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                notification::markAsRead
        );
        assertEquals("NOTIFICATION_ALREADY_READ", exception.getCode());
    }

    @Test
    @DisplayName("Reconstituir notificación desde persistencia")
    void reconstituteNotification() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant readAt = Instant.parse("2026-01-01T12:00:00Z");

        Notification notification = Notification.reconstitute(
                id, userId, NotificationType.LOW_STOCK, "Stock bajo",
                "Producto X tiene stock bajo", NotificationStatus.READ, createdAt, readAt
        );

        assertEquals(id, notification.getId());
        assertEquals(userId, notification.getUserId());
        assertEquals(NotificationType.LOW_STOCK, notification.getType());
        assertEquals("Stock bajo", notification.getTitle());
        assertEquals("Producto X tiene stock bajo", notification.getMessage());
        assertEquals(NotificationStatus.READ, notification.getStatus());
        assertEquals(createdAt, notification.getCreatedAt());
        assertEquals(readAt, notification.getReadAt());
        assertFalse(notification.isUnread());
    }

    @Test
    @DisplayName("Título y mensaje deben recortar espacios en blanco")
    void titleAndMessageShouldBeTrimmed() {
        Notification notification = Notification.create(
                userId, NotificationType.SYSTEM, "  Título con espacios  ", "  Mensaje con espacios  "
        );

        assertEquals("Título con espacios", notification.getTitle());
        assertEquals("Mensaje con espacios", notification.getMessage());
    }
}
