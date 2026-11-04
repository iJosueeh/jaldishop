package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.util.Objects;

public record ExecuteRefundCommand(
        String providerPaymentId,
        BigDecimal amount,
        String idempotencyKey
) {
    public ExecuteRefundCommand {
        Objects.requireNonNull(providerPaymentId, "providerPaymentId no puede ser nulo");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey no puede ser nulo");
    }
}
