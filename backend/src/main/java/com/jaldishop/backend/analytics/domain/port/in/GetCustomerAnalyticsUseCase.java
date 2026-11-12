package com.jaldishop.backend.analytics.domain.port.in;

import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;

import java.util.UUID;

public interface GetCustomerAnalyticsUseCase {
    CustomerAnalyticsReport getCustomerAnalytics(UUID userId, PeriodFilter period);
}
