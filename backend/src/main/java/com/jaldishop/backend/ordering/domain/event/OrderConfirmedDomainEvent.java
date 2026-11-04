package com.jaldishop.backend.ordering.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento ligero de dominio emitido cuando una orden ha sido confirmada exitosamente.
 *
 * <p>Diseñado como un <em>Thin Event</em>: no incluye datos sensibles de clientes
 * (dirección exacta, teléfono, tarjeta o desglose interno) y sirve únicamente como señal
 * para que el comerciante actualice su vista realizando una consulta oficial vía REST.</p>
 */
public record OrderConfirmedDomainEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String orderNumber,
        UUID storeId,
        BigDecimal totalAmount,
        String currency,
        Instant occurredAt
) {

    public OrderConfirmedDomainEvent {
        Objects.requireNonNull(eventId, "eventId no puede ser nulo");
        Objects.requireNonNull(eventType, "eventType no puede ser nulo");
        Objects.requireNonNull(orderId, "orderId no puede ser nulo");
        Objects.requireNonNull(orderNumber, "orderNumber no puede ser nulo");
        Objects.requireNonNull(storeId, "storeId no puede ser nulo");
        Objects.requireNonNull(totalAmount, "totalAmount no puede ser nulo");
        Objects.requireNonNull(currency, "currency no puede ser nulo");
        Objects.requireNonNull(occurredAt, "occurredAt no puede ser nulo");
    }

    public OrderConfirmedDomainEvent(UUID orderId, String orderNumber, UUID storeId, BigDecimal totalAmount, String currency) {
        this(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                orderId,
                orderNumber,
                storeId,
                totalAmount,
                currency,
                Instant.now()
        );
    }

}
