package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record ExecuteRefundCommand(
        String checkoutMode,
        String providerOrderId,
        String providerPaymentId,
        BigDecimal amount,
        String idempotencyKey
) {
    public ExecuteRefundCommand {
        Objects.requireNonNull(idempotencyKey, "idempotencyKey no puede ser nulo");
        if (checkoutMode == null || checkoutMode.isBlank()) {
            checkoutMode = "PREFERENCES";
        }
        if ("ORDERS".equalsIgnoreCase(checkoutMode)) {
            Objects.requireNonNull(providerOrderId, "providerOrderId no puede ser nulo para reembolsos en modo ORDERS");
        } else {
            Objects.requireNonNull(providerPaymentId, "providerPaymentId no puede ser nulo para reembolsos en modo PREFERENCES");
        }
    }

    public ExecuteRefundCommand(String providerPaymentId, BigDecimal amount, String idempotencyKey) {
        this("PREFERENCES", null, providerPaymentId, amount, idempotencyKey);
    }
}
