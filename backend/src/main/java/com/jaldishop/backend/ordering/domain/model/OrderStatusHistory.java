package com.jaldishop.backend.ordering.domain.model;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusHistory(
        UUID id,
        UUID orderId,
        UUID changedByUserId,
        OrderStatus status,
        String reason,
        Instant changedAt
) {

    public static OrderStatusHistory create(UUID orderId, UUID changedByUserId, OrderStatus status,
                                            String reason, Instant changedAt) {
        if (orderId == null) {
            throw new IllegalArgumentException("El ID de la orden es obligatorio para el historial de estado.");
        }
        if (status == null) {
            throw new IllegalArgumentException("El estado es obligatorio para el historial.");
        }
        return new OrderStatusHistory(
                UUID.randomUUID(),
                orderId,
                changedByUserId,
                status,
                reason,
                changedAt != null ? changedAt : Instant.now()
        );
    }

}
