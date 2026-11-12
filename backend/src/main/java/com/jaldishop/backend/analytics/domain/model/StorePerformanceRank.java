package com.jaldishop.backend.analytics.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Rendimiento comercial y financiero comparativo entre tiendas.
 */
public record StorePerformanceRank(
        UUID storeId,
        String storeName,
        long confirmedOrdersCount,
        BigDecimal commercialGmv,
        BigDecimal grossInflowAmount,
        BigDecimal settledRefundsAmount,
        BigDecimal netFinancialInflow
) {
    public StorePerformanceRank {
        commercialGmv = sanitize(commercialGmv);
        grossInflowAmount = sanitize(grossInflowAmount);
        settledRefundsAmount = sanitize(settledRefundsAmount);
        netFinancialInflow = sanitize(netFinancialInflow);
    }

    private static BigDecimal sanitize(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
