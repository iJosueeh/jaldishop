package com.jaldishop.backend.payment.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerPaymentHistoryResponse(
        UUID paymentId,
        String paymentStatus,
        String refundStatus,
        BigDecimal amount,
        String currency,
        String orderNumber,
        String orderStatus,
        Instant createdAt,
        Instant approvedAt,
        Instant refundedAt
) {}
