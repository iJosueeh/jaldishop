package com.jaldishop.backend.realtime.infrastructure.listener;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.realtime.application.service.RealtimeNotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderDomainEventListenerTest {

    @Mock
    private RealtimeNotificationPublisher realtimeNotificationPublisher;

    private OrderDomainEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderDomainEventListener(realtimeNotificationPublisher);
    }

    @Test
    @DisplayName("onOrderConfirmed: Debe delegar la publicación del evento de confirmación")
    void onOrderConfirmedDelegatesToPublisher() {
        OrderConfirmedDomainEvent event = new OrderConfirmedDomainEvent(
                UUID.randomUUID(),
                "JAL-2026-000001",
                UUID.randomUUID(),
                BigDecimal.valueOf(120.00),
                "PEN"
        );

        listener.onOrderConfirmed(event);

        verify(realtimeNotificationPublisher).publishOrderConfirmed(event);
    }

    @Test
    @DisplayName("onOrderStatusChanged: Debe delegar la publicación del evento de cambio de estado")
    void onOrderStatusChangedDelegatesToPublisher() {
        OrderStatusChangedDomainEvent event = new OrderStatusChangedDomainEvent(
                UUID.randomUUID(),
                "JAL-2026-000001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY
        );

        listener.onOrderStatusChanged(event);

        verify(realtimeNotificationPublisher).publishOrderStatusChanged(event);
    }

    @Test
    @DisplayName("onOrderConfirmed: Debe capturar excepciones del transporte WebSocket sin relanzarlas para no alterar la compra confirmada")
    void onOrderConfirmedHandlesPublisherExceptionGracefully() {
        OrderConfirmedDomainEvent event = new OrderConfirmedDomainEvent(
                UUID.randomUUID(),
                "JAL-2026-000001",
                UUID.randomUUID(),
                BigDecimal.valueOf(120.00),
                "PEN"
        );

        org.mockito.Mockito.doThrow(new org.springframework.messaging.MessagingException("Simulated socket error"))
                .when(realtimeNotificationPublisher).publishOrderConfirmed(event);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                listener.onOrderConfirmed(event)
        );
    }

    @Test
    @DisplayName("onOrderStatusChanged: Debe capturar excepciones del transporte WebSocket sin relanzarlas")
    void onOrderStatusChangedHandlesPublisherExceptionGracefully() {
        OrderStatusChangedDomainEvent event = new OrderStatusChangedDomainEvent(
                UUID.randomUUID(),
                "JAL-2026-000001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderStatus.IN_PREPARATION,
                OrderStatus.READY
        );

        org.mockito.Mockito.doThrow(new org.springframework.messaging.MessagingException("Simulated socket error"))
                .when(realtimeNotificationPublisher).publishOrderStatusChanged(event);

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                listener.onOrderStatusChanged(event)
        );
    }

}
