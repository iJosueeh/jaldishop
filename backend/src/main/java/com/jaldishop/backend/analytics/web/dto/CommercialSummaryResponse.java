package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.CommercialSummary;

import java.math.BigDecimal;

public record CommercialSummaryResponse(
        long historicalConfirmedOrdersCount,
        BigDecimal historicalConfirmedGmv,
        long activeOrdersCount,
        BigDecimal activeCommercialVolume,
        long completedOrdersCount,
        BigDecimal productsGrossSales,
        BigDecimal totalDiscounts,
        BigDecimal totalDeliveryFees,
        long cancelledOrdersCount,
        BigDecimal cancelledOrdersVolume,
        BigDecimal averageTicket
) {
    public static CommercialSummaryResponse from(CommercialSummary domain) {
        if (domain == null) {
            return null;
        }
        return new CommercialSummaryResponse(
                domain.historicalConfirmedOrdersCount(),
                domain.historicalConfirmedGmv(),
                domain.activeOrdersCount(),
                domain.activeCommercialVolume(),
                domain.completedOrdersCount(),
                domain.productsGrossSales(),
                domain.totalDiscounts(),
                domain.totalDeliveryFees(),
                domain.cancelledOrdersCount(),
                domain.cancelledOrdersVolume(),
                domain.averageTicket()
        );
    }
}
