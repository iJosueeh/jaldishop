package com.jaldishop.backend.notification.infrastructure.listener;
import com.jaldishop.backend.notification.application.command.*;
import com.jaldishop.backend.notification.application.service.*;
import com.jaldishop.backend.notification.domain.model.*;
import com.jaldishop.backend.notification.domain.enums.*;
import com.jaldishop.backend.notification.domain.repository.*;
import com.jaldishop.backend.notification.domain.port.*;


import com.jaldishop.backend.notification.application.command.CreateNotificationCommand;
import com.jaldishop.backend.notification.application.service.IsolatedOrderNotificationPersister;
import com.jaldishop.backend.notification.domain.model.Notification;
import com.jaldishop.backend.notification.domain.port.NotificationRecipientResolver;
import com.jaldishop.backend.notification.domain.enums.NotificationType;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Unit Tests: OrderNotificationEventListener")
class OrderNotificationEventListenerTest {

    private NotificationRecipientResolver recipientResolver;
    private IsolatedOrderNotificationPersister persister;
    private OrderNotificationEventListener listener;

    private UUID storeId;
    private UUID merchantUserId;
    private UUID orderId;
    private OrderConfirmedDomainEvent event;

    @BeforeEach
    void setUp() {
        recipientResolver = mock(NotificationRecipientResolver.class);
        persister = mock(IsolatedOrderNotificationPersister.class);
        listener = new OrderNotificationEventListener(recipientResolver, persister);

        storeId = UUID.randomUUID();
        merchantUserId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        event = new OrderConfirmedDomainEvent(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                orderId,
                "ORD-20261101-0001",
                storeId,
                new BigDecimal("150.00"),
                "PEN",
                Instant.now()
        );
    }

    @Test
    @DisplayName("Debe persistir notificación NEW_ORDER con título, mensaje y deduplication_key correctos")
    void shouldPersistNotificationSuccessfully() {
        when(recipientResolver.resolveMerchantUserId(storeId)).thenReturn(Optional.of(merchantUserId));

        Notification notification = Notification.create(
                merchantUserId,
                NotificationType.NEW_ORDER,
                "Nuevo pedido recibido: ORD-20261101-0001",
                "Has recibido una nueva orden. Ingresa a la sección de pedidos para gestionarla.",
                "NEW_ORDER:" + orderId
        );
        when(persister.persistIsolated(any())).thenReturn(notification);

        assertDoesNotThrow(() -> listener.onOrderConfirmed(event));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(persister).persistIsolated(captor.capture());

        CreateNotificationCommand command = captor.getValue();
        assertThat(command.userId()).isEqualTo(merchantUserId);
        assertThat(command.type()).isEqualTo(NotificationType.NEW_ORDER);
        assertThat(command.title()).isEqualTo("Nuevo pedido recibido: ORD-20261101-0001");
        assertThat(command.message()).isEqualTo("Has recibido una nueva orden. Ingresa a la sección de pedidos para gestionarla.");
        assertThat(command.deduplicationKey()).isEqualTo("NEW_ORDER:" + orderId);
        // Verificar que no contiene datos personales ni montos en el título o mensaje
        assertThat(command.title()).doesNotContain("150.00").doesNotContain("PEN");
        assertThat(command.message()).doesNotContain("150.00").doesNotContain("PEN");
    }

    @Test
    @DisplayName("Debe omitir la persistencia si no se resuelve el merchantUserId de la tienda")
    void shouldSkipPersistenceWhenStoreOwnerNotFound() {
        when(recipientResolver.resolveMerchantUserId(storeId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> listener.onOrderConfirmed(event));

        verify(persister, never()).persistIsolated(any());
    }

    @Test
    @DisplayName("Debe manejar de forma idempotente una colisión de clave de deduplicación sin propagar excepción")
    void shouldHandleDeduplicationKeyCollisionGracefully() {
        when(recipientResolver.resolveMerchantUserId(storeId)).thenReturn(Optional.of(merchantUserId));

        SQLException sqlEx = new SQLException("duplicate key value", "23505");
        ConstraintViolationException cve = new ConstraintViolationException(
                "could not execute statement",
                sqlEx,
                sqlEx.getMessage(),
                "uq_notifications_deduplication_key"
        );
        when(persister.persistIsolated(any())).thenThrow(cve);

        assertDoesNotThrow(() -> listener.onOrderConfirmed(event));
    }

    @Test
    @DisplayName("Debe capturar errores inesperados de base de datos sin propagar excepción al hilo de checkout")
    void shouldCatchUnexpectedDatabaseExceptionsWithoutThrowing() {
        when(recipientResolver.resolveMerchantUserId(storeId)).thenReturn(Optional.of(merchantUserId));
        when(persister.persistIsolated(any())).thenThrow(new RuntimeException("Pool connection exhausted"));

        assertDoesNotThrow(() -> listener.onOrderConfirmed(event));
    }

}
