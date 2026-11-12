package com.jaldishop.backend.analytics.domain.port.in;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;

import java.util.UUID;

/**
 * Caso de uso para obtener métricas analíticas globales de la plataforma.
 * Sigue la convención execute(...) de los servicios del backend JaldiShop.
 */
public interface GetAdminPlatformAnalyticsUseCase {

    AdminPlatformAnalyticsReport execute(PeriodFilter period, UUID filterStoreId);

    default AdminPlatformAnalyticsReport getPlatformAnalytics(PeriodFilter period, UUID filterStoreId) {
        return execute(period, filterStoreId);
    }
}
