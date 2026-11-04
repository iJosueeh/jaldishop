package com.jaldishop.backend.ordering.domain.event;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento ligero de dominio emitido cuando cambia el estado de una orden.
 *
 * <p>Diseñado como un <em>Thin Event</em> destinado al cliente propietario de la orden.
 * Permite que el frontend del cliente sepa qué pedido avanzó en su ciclo de vida y
 * consulte los detalles oficiales mediante el endpoint REST correspondiente.</p>
 */
public record OrderStatusChangedDomainEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String orderNumber,
        UUID customerId,
        UUID storeId,
        OrderStatus previousStatus,
        OrderStatus newStatus,
        Instant occurredAt
) {

    public OrderStatusChangedDomainEvent {
        Objects.requireNonNull(eventId, "eventId no puede ser nulo");
        Objects.requireNonNull(eventType, "eventType no puede ser nulo");
        Objects.requireNonNull(orderId, "orderId no puede ser nulo");
        Objects.requireNonNull(orderNumber, "orderNumber no puede ser nulo");
        Objects.requireNonNull(customerId, "customerId no puede ser nulo");
        Objects.requireNonNull(storeId, "storeId no puede ser nulo");
        Objects.requireNonNull(newStatus, "newStatus no puede ser nulo");
        Objects.requireNonNull(occurredAt, "occurredAt no puede ser nulo");
    }

    public OrderStatusChangedDomainEvent(
            UUID orderId,
            String orderNumber,
            UUID customerId,
            UUID storeId,
            OrderStatus previousStatus,
            OrderStatus newStatus
    ) {
        this(
                UUID.randomUUID(),
                "ORDER_STATUS_CHANGED",
                orderId,
                orderNumber,
                customerId,
                storeId,
                previousStatus,
                newStatus,
                Instant.now()
        );
    }

}
