package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Resultado de la creación de una orden en Orders API devuelto por el gateway.
 */
public record OrderResult(
        String orderId,
        String checkoutUrl,
        String status,
        BigDecimal totalAmount
) {
    public OrderResult {
        Objects.requireNonNull(orderId, "orderId no puede ser nulo");
        Objects.requireNonNull(checkoutUrl, "checkoutUrl no puede ser nulo");
        if (orderId.isBlank()) {
            throw new IllegalArgumentException("orderId no puede estar en blanco");
        }
        if (checkoutUrl.isBlank()) {
            throw new IllegalArgumentException("checkoutUrl no puede estar en blanco");
        }
    }
}
