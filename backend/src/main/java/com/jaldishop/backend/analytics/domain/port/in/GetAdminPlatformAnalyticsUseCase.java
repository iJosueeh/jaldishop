package com.jaldishop.backend.analytics.domain.port.in;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;

import java.util.UUID;

public interface GetAdminPlatformAnalyticsUseCase {
    AdminPlatformAnalyticsReport getPlatformAnalytics(PeriodFilter period, UUID filterStoreId);
}
