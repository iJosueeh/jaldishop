package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.CustomerSpendingSummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerSpendingSummaryResponse(
        UUID userId,
        long totalOrdersCount,
        BigDecimal totalSpentAmount,
        BigDecimal totalRefundedAmount,
        Instant firstOrderAt,
        Instant lastOrderAt
) {
    public static CustomerSpendingSummaryResponse from(CustomerSpendingSummary domain) {
        if (domain == null) {
            return null;
        }
        return new CustomerSpendingSummaryResponse(
                domain.userId(),
                domain.totalOrdersCount(),
                domain.totalSpentAmount(),
                domain.totalRefundedAmount(),
                domain.firstOrderAt(),
                domain.lastOrderAt()
        );
    }
}
