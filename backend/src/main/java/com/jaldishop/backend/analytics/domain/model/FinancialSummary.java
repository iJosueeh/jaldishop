package com.jaldishop.backend.analytics.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Resumen de flujos monetarios capturados y restituidos en pasarela (Mercado Pago).
 * <p>
 * IMPORTANTE: No representa liquidación bancaria ni saldo disponible en cuentas corrientes.
 * Registra entradas brutas por {@code approved_at} y salidas efectivas por {@code refunded_at}.
 * </p>
 */
public record FinancialSummary(
        BigDecimal grossInflowAmount,
        long approvedPaymentsCount,
        BigDecimal settledRefundsAmount,
        long settledRefundsCount,
        BigDecimal netFinancialInflow,
        BigDecimal orderLinkedPaymentsAmount,
        long orderLinkedPaymentsCount,
        BigDecimal orphanPaymentsAmount,
        long orphanPaymentsCount
) {
    public static final BigDecimal ZERO_MONEY = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public FinancialSummary {
        grossInflowAmount = sanitize(grossInflowAmount);
        settledRefundsAmount = sanitize(settledRefundsAmount);
        orderLinkedPaymentsAmount = sanitize(orderLinkedPaymentsAmount);
        orphanPaymentsAmount = sanitize(orphanPaymentsAmount);

        if (netFinancialInflow == null) {
            netFinancialInflow = grossInflowAmount.subtract(settledRefundsAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            netFinancialInflow = sanitize(netFinancialInflow);
        }
    }

    public static FinancialSummary empty() {
        return new FinancialSummary(
                ZERO_MONEY, 0L,
                ZERO_MONEY, 0L,
                ZERO_MONEY,
                ZERO_MONEY, 0L,
                ZERO_MONEY, 0L
        );
    }

    public static FinancialSummary of(
            BigDecimal grossInflowAmount,
            long approvedPaymentsCount,
            BigDecimal settledRefundsAmount,
            long settledRefundsCount,
            BigDecimal orderLinkedPaymentsAmount,
            long orderLinkedPaymentsCount,
            BigDecimal orphanPaymentsAmount,
            long orphanPaymentsCount
    ) {
        BigDecimal sanitizedInflow = sanitize(grossInflowAmount);
        BigDecimal sanitizedRefunds = sanitize(settledRefundsAmount);
        BigDecimal net = sanitizedInflow.subtract(sanitizedRefunds).setScale(2, RoundingMode.HALF_UP);

        return new FinancialSummary(
                sanitizedInflow,
                approvedPaymentsCount,
                sanitizedRefunds,
                settledRefundsCount,
                net,
                sanitize(orderLinkedPaymentsAmount),
                orderLinkedPaymentsCount,
                sanitize(orphanPaymentsAmount),
                orphanPaymentsCount
        );
    }

    private static BigDecimal sanitize(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
