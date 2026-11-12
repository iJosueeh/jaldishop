package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;

import java.util.List;
import java.util.UUID;

public record CustomerAnalyticsSummaryResponse(
        UUID userId,
        PeriodResponse period,
        CustomerSpendingSummaryResponse spending,
        List<ProductSalesRankResponse> topPurchasedProducts
) {
    public static CustomerAnalyticsSummaryResponse from(CustomerAnalyticsReport report) {
        return new CustomerAnalyticsSummaryResponse(
                report.userId(),
                PeriodResponse.from(report.period()),
                CustomerSpendingSummaryResponse.from(report.spending()),
                report.topPurchasedProducts().stream().map(ProductSalesRankResponse::from).toList()
        );
    }
}
