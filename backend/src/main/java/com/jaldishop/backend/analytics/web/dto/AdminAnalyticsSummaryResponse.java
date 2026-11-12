package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;

import java.util.List;
import java.util.UUID;

public record AdminAnalyticsSummaryResponse(
        PeriodResponse period,
        UUID filterStoreId,
        CommercialSummaryResponse commercial,
        FinancialSummaryResponse financial,
        List<ProductSalesRankResponse> topProducts,
        List<StorePerformanceRankResponse> storeRankings,
        CapacityDemandSummaryResponse capacityDemand
) {
    public static AdminAnalyticsSummaryResponse from(AdminPlatformAnalyticsReport report) {
        return new AdminAnalyticsSummaryResponse(
                PeriodResponse.from(report.period()),
                report.filterStoreId(),
                CommercialSummaryResponse.from(report.commercial()),
                FinancialSummaryResponse.from(report.financial()),
                report.topProducts().stream().map(ProductSalesRankResponse::from).toList(),
                report.storeRankings().stream().map(StorePerformanceRankResponse::from).toList(),
                CapacityDemandSummaryResponse.from(report.capacityDemand())
        );
    }
}
