package com.jaldishop.backend.realtime.application.service;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RealtimeNotificationPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private RealtimeNotificationPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RealtimeNotificationPublisher(messagingTemplate);
    }

    @Test
    @DisplayName("publishOrderConfirmed: Debe enviar el evento al tópico de tienda /topic/stores/{storeId}/orders")
    void publishOrderConfirmedSendsToStoreTopic() {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        OrderConfirmedDomainEvent event = new OrderConfirmedDomainEvent(
                orderId,
                "JAL-2026-000001",
                storeId,
                BigDecimal.valueOf(99.90),
                "PEN"
        );

        publisher.publishOrderConfirmed(event);

        String expectedDestination = "/topic/stores/" + storeId + "/orders";
        ArgumentCaptor<OrderConfirmedDomainEvent> payloadCaptor = ArgumentCaptor.forClass(OrderConfirmedDomainEvent.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(expectedDestination), payloadCaptor.capture());

        OrderConfirmedDomainEvent captured = payloadCaptor.getValue();
        assertEquals(orderId, captured.orderId());
        assertEquals("JAL-2026-000001", captured.orderNumber());
        assertEquals(storeId, captured.storeId());
        assertEquals(BigDecimal.valueOf(99.90), captured.totalAmount());
        assertNotNull(captured.eventId());
        assertNotNull(captured.occurredAt());
    }

    @Test
    @DisplayName("publishOrderStatusChanged: Debe enviar el evento a la cola privada del cliente /queue/orders y al tópico de tienda /topic/stores/{storeId}/orders")
    void publishOrderStatusChangedSendsToCustomerQueueAndStoreTopic() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        OrderStatusChangedDomainEvent event = new OrderStatusChangedDomainEvent(
                orderId,
                "JAL-2026-000001",
                customerId,
                storeId,
                OrderStatus.CONFIRMED,
                OrderStatus.IN_PREPARATION
        );

        publisher.publishOrderStatusChanged(event);

        ArgumentCaptor<OrderStatusChangedDomainEvent> customerCaptor = ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(messagingTemplate).convertAndSendToUser(
                org.mockito.ArgumentMatchers.eq(customerId.toString()),
                org.mockito.ArgumentMatchers.eq("/queue/orders"),
                customerCaptor.capture()
        );

        OrderStatusChangedDomainEvent capturedCustomer = customerCaptor.getValue();
        assertEquals(orderId, capturedCustomer.orderId());
        assertEquals(customerId, capturedCustomer.customerId());
        assertEquals(OrderStatus.CONFIRMED, capturedCustomer.previousStatus());
        assertEquals(OrderStatus.IN_PREPARATION, capturedCustomer.newStatus());

        String expectedStoreTopic = "/topic/stores/" + storeId + "/orders";
        ArgumentCaptor<OrderStatusChangedDomainEvent> storeCaptor = ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(expectedStoreTopic),
                storeCaptor.capture()
        );

        OrderStatusChangedDomainEvent capturedStore = storeCaptor.getValue();
        assertEquals(orderId, capturedStore.orderId());
        assertEquals(storeId, capturedStore.storeId());
        assertEquals(OrderStatus.CONFIRMED, capturedStore.previousStatus());
        assertEquals(OrderStatus.IN_PREPARATION, capturedStore.newStatus());
    }

}
