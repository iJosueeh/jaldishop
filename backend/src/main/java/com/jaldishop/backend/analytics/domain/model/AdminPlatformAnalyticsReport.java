package com.jaldishop.backend.analytics.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Reporte global o filtrado de la plataforma para el rol ADMIN.
 */
public record AdminPlatformAnalyticsReport(
        PeriodFilter period,
        UUID filterStoreId,
        CommercialSummary commercial,
        FinancialSummary financial,
        List<ProductSalesRank> topProducts,
        List<StorePerformanceRank> storeRankings,
        CapacityDemandSummary capacityDemand
) {
}
