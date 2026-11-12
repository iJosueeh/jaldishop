package com.jaldishop.backend.analytics.domain.port.in;

import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;

import java.util.UUID;

public interface GetMerchantAnalyticsUseCase {
    MerchantAnalyticsReport getMerchantAnalytics(UUID storeId, PeriodFilter period);
}
