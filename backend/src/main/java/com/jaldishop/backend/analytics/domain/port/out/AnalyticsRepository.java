package com.jaldishop.backend.analytics.domain.port.out;

import com.jaldishop.backend.analytics.domain.model.*;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida para consultas analíticas agregadas directas a la base de datos.
 */
public interface AnalyticsRepository {

    CommercialSummary findCommercialSummary(PeriodFilter period, UUID storeId, UUID userId);

    FinancialSummary findFinancialSummary(PeriodFilter period, UUID storeId, UUID userId);

    List<ProductSalesRank> findTopSellingProducts(PeriodFilter period, UUID storeId, UUID userId, int limit);

    List<StorePerformanceRank> findStoreRankings(PeriodFilter period, int limit);

    CapacityDemandSummary findCapacityDemand(PeriodFilter period, UUID storeId);

    CustomerSpendingSummary findCustomerSpending(PeriodFilter period, UUID userId);
}
