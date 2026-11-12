package com.jaldishop.backend.analytics.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Resumen individual de compras para el rol CUSTOMER.
 */
public record CustomerSpendingSummary(
        UUID userId,
        long totalOrdersCount,
        BigDecimal totalSpentAmount,
        BigDecimal totalRefundedAmount,
        Instant firstOrderAt,
        Instant lastOrderAt
) {
    public CustomerSpendingSummary {
        totalSpentAmount = Objects.requireNonNullElse(totalSpentAmount, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        totalRefundedAmount = Objects.requireNonNullElse(totalRefundedAmount, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
