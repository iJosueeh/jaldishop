package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Resultado desacoplado de la consulta de una orden en Orders API (/v1/orders/{id}).
 */
public record OrderQueryResult(
        String orderId,
        String status,
        String statusDetail,
        BigDecimal totalAmount,
        BigDecimal totalPaidAmount,
        String externalReference,
        List<OrderPaymentItem> payments,
        Long collectorId,
        Boolean liveMode
) {
    public OrderQueryResult {
        Objects.requireNonNull(orderId, "orderId no puede ser nulo");
        Objects.requireNonNull(status, "status no puede ser nulo");
        if (orderId.isBlank()) {
            throw new IllegalArgumentException("orderId no puede estar en blanco");
        }
        if (payments == null) {
            payments = List.of();
        }
    }

    public record OrderPaymentItem(
            String providerPaymentId,
            String status,
            String statusDetail,
            BigDecimal transactionAmount,
            String currency,
            String paymentMethodId,
            String paymentTypeId,
            Instant dateApproved
    ) {
        public OrderPaymentItem {
            Objects.requireNonNull(providerPaymentId, "providerPaymentId no puede ser nulo");
        }
    }
}
