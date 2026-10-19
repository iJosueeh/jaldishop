package com.jaldishop.backend.ordering.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(
        UUID id,
        UUID orderId,
        UUID variantId,
        String productName,
        String variantName,
        int quantity,
        BigDecimal unitPriceAmount,
        BigDecimal subtotalAmount
) {

    public static OrderItem create(UUID orderId, Order.ItemData data) {
        if (orderId == null) {
            throw new IllegalArgumentException("El ID de la orden es obligatorio para crear el detalle.");
        }
        if (data == null) {
            throw new IllegalArgumentException("Los datos del ítem son obligatorios.");
        }
        BigDecimal unitPrice = data.unitPriceAmount() != null ? data.unitPriceAmount() : BigDecimal.ZERO;
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(data.quantity()));

        return new OrderItem(
                UUID.randomUUID(),
                orderId,
                data.variantId(),
                data.productName(),
                data.variantName(),
                data.quantity(),
                unitPrice,
                subtotal
        );
    }

}