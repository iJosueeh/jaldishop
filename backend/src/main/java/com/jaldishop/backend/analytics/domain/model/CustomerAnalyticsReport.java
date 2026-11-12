package com.jaldishop.backend.analytics.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Reporte de compras personales para el rol CUSTOMER.
 */
public record CustomerAnalyticsReport(
        UUID userId,
        PeriodFilter period,
        CustomerSpendingSummary spending,
        List<ProductSalesRank> topPurchasedProducts
) {
}
