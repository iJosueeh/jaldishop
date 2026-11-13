package com.jaldishop.backend.payment.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerPaymentStatusResponse(
        UUID paymentId,
        String paymentStatus,
        boolean orderCreated,
        String orderNumber,
        String orderStatus,
        boolean hasIncidence,
        String incidenceCode,
        String incidenceMessage,
        String refundStatus,
        BigDecimal amount,
        String currency,
        Instant createdAt,
        Instant approvedAt
) {}
