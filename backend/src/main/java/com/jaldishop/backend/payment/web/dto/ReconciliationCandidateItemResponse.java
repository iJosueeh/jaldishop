package com.jaldishop.backend.payment.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationCandidateItemResponse(
        UUID attemptId,
        UUID paymentId,
        String providerOrderId,
        String providerPaymentId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String storeName,
        String customerEmail,
        String customerName,
        Instant approvedAt,
        String incidenceType
) {}
