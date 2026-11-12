package com.jaldishop.backend.analytics.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Reporte analítico completo para el rol MERCHANT.
 */
public record MerchantAnalyticsReport(
        UUID storeId,
        PeriodFilter period,
        CommercialSummary commercial,
        FinancialSummary financial,
        List<ProductSalesRank> topProducts,
        CapacityDemandSummary capacityDemand
) {
}
