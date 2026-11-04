package com.jaldishop.backend.checkout.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResume(
        UUID paymentId,
        UUID capacityReservationId,
        String status,
        Instant approvedAt,
        BigDecimal amount,
        String currency,
        String fulfillmentType,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        BigDecimal productsSubtotalAmount,
        BigDecimal discountAmount,
        String discountCode,
        BigDecimal deliveryFeeAmount,
        BigDecimal includedTaxAmount,
        String itemsSnapshot
) {
    public PaymentResume(
            UUID paymentId,
            UUID capacityReservationId,
            String status,
            Instant approvedAt,
            BigDecimal amount,
            String currency
    ) {
        this(
                paymentId,
                capacityReservationId,
                status,
                approvedAt,
                amount,
                currency,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                amount,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null
        );
    }
}
