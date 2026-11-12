package com.jaldishop.backend.analytics.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Resumen de actividad comercial.
 * <p>
 * Separa estrictamente el GMV histórico confirmado (pactado originalmente al confirmar el pedido)
 * del volumen comercial activo/vigente (pedidos que no han sido cancelados posteriormente).
 * Las cancelaciones posteriores se auditan de forma independiente en {@code cancelledOrdersVolume}.
 * </p>
 */
public record CommercialSummary(
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
    public static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public CommercialSummary {
        historicalConfirmedGmv = sanitize(historicalConfirmedGmv);
        activeCommercialVolume = sanitize(activeCommercialVolume);
        productsGrossSales = sanitize(productsGrossSales);
        totalDiscounts = sanitize(totalDiscounts);
        totalDeliveryFees = sanitize(totalDeliveryFees);
        cancelledOrdersVolume = sanitize(cancelledOrdersVolume);

        if (averageTicket == null) {
            if (historicalConfirmedOrdersCount > 0) {
                averageTicket = historicalConfirmedGmv.divide(
                        BigDecimal.valueOf(historicalConfirmedOrdersCount), 2, RoundingMode.HALF_UP
                );
            } else {
                averageTicket = ZERO_MONEY;
            }
        } else {
            averageTicket = sanitize(averageTicket);
        }
    }

    public static CommercialSummary empty() {
        return new CommercialSummary(
                0L,
                ZERO_MONEY,
                0L,
                ZERO_MONEY,
                0L,
                ZERO_MONEY,
                ZERO_MONEY,
                ZERO_MONEY,
                0L,
                ZERO_MONEY,
                ZERO_MONEY
        );
    }

    public static CommercialSummary of(
            long historicalConfirmedOrdersCount,
            BigDecimal historicalConfirmedGmv,
            long activeOrdersCount,
            BigDecimal activeCommercialVolume,
            long completedOrdersCount,
            BigDecimal productsGrossSales,
            BigDecimal totalDiscounts,
            BigDecimal totalDeliveryFees,
            long cancelledOrdersCount,
            BigDecimal cancelledOrdersVolume
    ) {
        BigDecimal gmv = sanitize(historicalConfirmedGmv);
        BigDecimal ticket = historicalConfirmedOrdersCount > 0
                ? gmv.divide(BigDecimal.valueOf(historicalConfirmedOrdersCount), 2, RoundingMode.HALF_UP)
                : ZERO_MONEY;

        return new CommercialSummary(
                historicalConfirmedOrdersCount,
                gmv,
                activeOrdersCount,
                sanitize(activeCommercialVolume),
                completedOrdersCount,
                sanitize(productsGrossSales),
                sanitize(totalDiscounts),
                sanitize(totalDeliveryFees),
                cancelledOrdersCount,
                sanitize(cancelledOrdersVolume),
                ticket
        );
    }

    private static BigDecimal sanitize(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
