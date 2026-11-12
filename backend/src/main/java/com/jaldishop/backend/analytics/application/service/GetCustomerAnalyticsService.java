package com.jaldishop.backend.analytics.application.service;

import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CustomerSpendingSummary;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;
import com.jaldishop.backend.analytics.domain.model.ProductSalesRank;
import com.jaldishop.backend.analytics.domain.port.in.GetCustomerAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetCustomerAnalyticsService implements GetCustomerAnalyticsUseCase {

    private static final int DEFAULT_PURCHASED_PRODUCTS_LIMIT = 10;
    private final AnalyticsRepository analyticsRepository;

    public GetCustomerAnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = Objects.requireNonNull(analyticsRepository, "analyticsRepository no puede ser nulo");
    }

    @Override
    public CustomerAnalyticsReport getCustomerAnalytics(UUID userId, PeriodFilter period) {
        Objects.requireNonNull(userId, "El identificador del usuario (userId) es obligatorio.");
        Objects.requireNonNull(period, "El periodo de consulta es obligatorio.");

        CustomerSpendingSummary spending = analyticsRepository.findCustomerSpending(period, userId);
        List<ProductSalesRank> topPurchasedProducts = analyticsRepository.findTopSellingProducts(
                period, null, userId, DEFAULT_PURCHASED_PRODUCTS_LIMIT
        );

        return new CustomerAnalyticsReport(
                userId,
                period,
                spending,
                topPurchasedProducts
        );
    }
}
