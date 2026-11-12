package com.jaldishop.backend.analytics.application.service;

import com.jaldishop.backend.analytics.domain.model.*;
import com.jaldishop.backend.analytics.domain.port.in.GetMerchantAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetMerchantAnalyticsService implements GetMerchantAnalyticsUseCase {

    private static final int DEFAULT_TOP_PRODUCTS_LIMIT = 10;
    private final AnalyticsRepository analyticsRepository;

    public GetMerchantAnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = Objects.requireNonNull(analyticsRepository, "analyticsRepository no puede ser nulo");
    }

    @Override
    public MerchantAnalyticsReport execute(UUID storeId, PeriodFilter period) {
        Objects.requireNonNull(storeId, "El identificador de la tienda (storeId) es obligatorio.");
        Objects.requireNonNull(period, "El periodo de consulta es obligatorio.");

        CommercialSummary commercial = analyticsRepository.findCommercialSummary(period, storeId, null);
        FinancialSummary financial = analyticsRepository.findFinancialSummary(period, storeId, null);
        List<ProductSalesRank> topProducts = analyticsRepository.findTopSellingProducts(period, storeId, null, DEFAULT_TOP_PRODUCTS_LIMIT);
        CapacityDemandSummary capacityDemand = analyticsRepository.findCapacityDemand(period, storeId);

        return new MerchantAnalyticsReport(
                storeId,
                period,
                commercial,
                financial,
                topProducts,
                capacityDemand
        );
    }

    @Override
    public MerchantAnalyticsReport getMerchantAnalytics(UUID storeId, PeriodFilter period) {
        return execute(storeId, period);
    }
}
