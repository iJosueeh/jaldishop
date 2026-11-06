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
        List<OrderRefundItem> refunds,
        Long collectorId,
        Boolean liveMode,
        String checkoutUrl
) {
    public OrderQueryResult(String orderId, String status, String statusDetail, BigDecimal totalAmount,
            BigDecimal totalPaidAmount, String externalReference, List<OrderPaymentItem> payments,
            List<OrderRefundItem> refunds, Long collectorId, Boolean liveMode) {
        this(orderId, status, statusDetail, totalAmount, totalPaidAmount, externalReference,
                payments, refunds, collectorId, liveMode, null);
    }

    public OrderQueryResult {
        Objects.requireNonNull(orderId, "orderId no puede ser nulo");
        Objects.requireNonNull(status, "status no puede ser nulo");
        if (orderId.isBlank()) {
            throw new IllegalArgumentException("orderId no puede estar en blanco");
        }
        if (payments == null) {
            payments = List.of();
        }
        if (refunds == null) {
            refunds = List.of();
        }
    }

    public OrderQueryResult(
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
        this(orderId, status, statusDetail, totalAmount, totalPaidAmount, externalReference, payments, List.of(), collectorId, liveMode);
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

    public record OrderRefundItem(
            String providerRefundId,
            String providerPaymentId,
            BigDecimal amount,
            String status,
            Instant dateCreated
    ) {
        public OrderRefundItem {
            Objects.requireNonNull(providerRefundId, "providerRefundId no puede ser nulo");
        }
    }
}
