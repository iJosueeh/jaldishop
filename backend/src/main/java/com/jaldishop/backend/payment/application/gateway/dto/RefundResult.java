package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResult(
        String providerRefundId,
        String providerPaymentId,
        BigDecimal amount,
        String status,
        Instant dateCreated
) {
}
