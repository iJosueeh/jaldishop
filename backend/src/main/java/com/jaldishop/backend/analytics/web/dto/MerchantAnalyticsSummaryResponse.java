package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;

import java.util.List;
import java.util.UUID;

public record MerchantAnalyticsSummaryResponse(
        UUID storeId,
        PeriodResponse period,
        CommercialSummaryResponse commercial,
        FinancialSummaryResponse financial,
        List<ProductSalesRankResponse> topProducts,
        CapacityDemandSummaryResponse capacityDemand
) {
    public static MerchantAnalyticsSummaryResponse from(MerchantAnalyticsReport report) {
        return new MerchantAnalyticsSummaryResponse(
                report.storeId(),
                PeriodResponse.from(report.period()),
                CommercialSummaryResponse.from(report.commercial()),
                FinancialSummaryResponse.from(report.financial()),
                report.topProducts().stream().map(ProductSalesRankResponse::from).toList(),
                CapacityDemandSummaryResponse.from(report.capacityDemand())
        );
    }
}
