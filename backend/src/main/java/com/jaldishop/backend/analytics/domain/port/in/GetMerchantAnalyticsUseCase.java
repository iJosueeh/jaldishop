package com.jaldishop.backend.analytics.domain.port.in;

import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;

import java.util.UUID;

/**
 * Caso de uso para obtener métricas analíticas de comercios.
 * Sigue la convención execute(...) de los servicios del backend JaldiShop.
 */
public interface GetMerchantAnalyticsUseCase {

    MerchantAnalyticsReport execute(UUID storeId, PeriodFilter period);

    default MerchantAnalyticsReport getMerchantAnalytics(UUID storeId, PeriodFilter period) {
        return execute(storeId, period);
    }
}
