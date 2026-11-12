package com.jaldishop.backend.analytics.application.service;

import com.jaldishop.backend.analytics.domain.model.*;
import com.jaldishop.backend.analytics.domain.port.in.GetAdminPlatformAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminPlatformAnalyticsService implements GetAdminPlatformAnalyticsUseCase {

    private static final int DEFAULT_TOP_PRODUCTS_LIMIT = 20;
    private static final int DEFAULT_TOP_STORES_LIMIT = 20;

    private final AnalyticsRepository analyticsRepository;

    public GetAdminPlatformAnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = Objects.requireNonNull(analyticsRepository, "analyticsRepository no puede ser nulo");
    }

    @Override
    public AdminPlatformAnalyticsReport execute(PeriodFilter period, UUID filterStoreId) {
        Objects.requireNonNull(period, "El periodo de consulta es obligatorio.");

        CommercialSummary commercial = analyticsRepository.findCommercialSummary(period, filterStoreId, null);
        FinancialSummary financial = analyticsRepository.findFinancialSummary(period, filterStoreId, null);
        List<ProductSalesRank> topProducts = analyticsRepository.findTopSellingProducts(period, filterStoreId, null, DEFAULT_TOP_PRODUCTS_LIMIT);
        List<StorePerformanceRank> storeRankings = analyticsRepository.findStoreRankings(period, DEFAULT_TOP_STORES_LIMIT);
        CapacityDemandSummary capacityDemand = analyticsRepository.findCapacityDemand(period, filterStoreId);

        return new AdminPlatformAnalyticsReport(
                period,
                filterStoreId,
                commercial,
                financial,
                topProducts,
                storeRankings,
                capacityDemand
        );
    }

    @Override
    public AdminPlatformAnalyticsReport getPlatformAnalytics(PeriodFilter period, UUID filterStoreId) {
        return execute(period, filterStoreId);
    }
}
