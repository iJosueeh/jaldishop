package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.FinancialSummary;

import java.math.BigDecimal;

public record FinancialSummaryResponse(
        BigDecimal grossInflowAmount,
        long approvedPaymentsCount,
        BigDecimal settledRefundsAmount,
        long settledRefundsCount,
        BigDecimal netFinancialInflow,
        BigDecimal orderLinkedPaymentsAmount,
        long orderLinkedPaymentsCount,
        BigDecimal orphanPaymentsAmount,
        long orphanPaymentsCount,
        String disclosure
) {
    public static final String DEFAULT_DISCLOSURE =
            "Fondos capturados y restituidos en pasarela de pagos (Mercado Pago). No representa liquidación bancaria ni desembolso en cuentas corrientes.";

    public static FinancialSummaryResponse from(FinancialSummary domain) {
        if (domain == null) {
            return null;
        }
        return new FinancialSummaryResponse(
                domain.grossInflowAmount(),
                domain.approvedPaymentsCount(),
                domain.settledRefundsAmount(),
                domain.settledRefundsCount(),
                domain.netFinancialInflow(),
                domain.orderLinkedPaymentsAmount(),
                domain.orderLinkedPaymentsCount(),
                domain.orphanPaymentsAmount(),
                domain.orphanPaymentsCount(),
                DEFAULT_DISCLOSURE
        );
    }
}
