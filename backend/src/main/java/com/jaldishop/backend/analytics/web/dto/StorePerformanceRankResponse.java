package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.StorePerformanceRank;

import java.math.BigDecimal;
import java.util.UUID;

public record StorePerformanceRankResponse(
        UUID storeId,
        String storeName,
        long confirmedOrdersCount,
        BigDecimal commercialGmv,
        BigDecimal grossInflowAmount,
        BigDecimal settledRefundsAmount,
        BigDecimal netFinancialInflow
) {
    public static StorePerformanceRankResponse from(StorePerformanceRank domain) {
        return new StorePerformanceRankResponse(
                domain.storeId(),
                domain.storeName(),
                domain.confirmedOrdersCount(),
                domain.commercialGmv(),
                domain.grossInflowAmount(),
                domain.settledRefundsAmount(),
                domain.netFinancialInflow()
        );
    }
}
