package com.jaldishop.backend.payment.application.gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Resultado desacoplado de la consulta de un pago en la pasarela externa.
 */
public record PaymentQueryResult(
        String providerPaymentId,
        String status,
        String statusDetail,
        BigDecimal transactionAmount,
        String currency,
        String paymentMethodId,
        String paymentTypeId,
        String externalReference,
        Instant dateApproved,
        Long collectorId,
        Boolean liveMode
) {
    public PaymentQueryResult(
            String providerPaymentId,
            String status,
            String statusDetail,
            BigDecimal transactionAmount,
            String currency,
            String paymentMethodId,
            String paymentTypeId,
            String externalReference,
            Instant dateApproved
    ) {
        this(providerPaymentId, status, statusDetail, transactionAmount, currency, paymentMethodId, paymentTypeId, externalReference, dateApproved, null, null);
    }

    public PaymentQueryResult {
        Objects.requireNonNull(providerPaymentId, "providerPaymentId no puede ser nulo");
        Objects.requireNonNull(status, "status no puede ser nulo");
        if (providerPaymentId.isBlank()) {
            throw new IllegalArgumentException("providerPaymentId no puede estar en blanco");
        }
        if (status.isBlank()) {
            throw new IllegalArgumentException("status no puede estar en blanco");
        }
    }
}
