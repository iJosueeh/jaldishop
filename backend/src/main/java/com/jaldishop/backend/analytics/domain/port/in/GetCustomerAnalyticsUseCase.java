package com.jaldishop.backend.analytics.domain.port.in;

import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;

import java.util.UUID;

/**
 * Caso de uso para obtener métricas analíticas de clientes.
 * Sigue la convención execute(...) de los servicios del backend JaldiShop.
 */
public interface GetCustomerAnalyticsUseCase {

    CustomerAnalyticsReport execute(UUID userId, PeriodFilter period);

    default CustomerAnalyticsReport getCustomerAnalytics(UUID userId, PeriodFilter period) {
        return execute(userId, period);
    }
}
