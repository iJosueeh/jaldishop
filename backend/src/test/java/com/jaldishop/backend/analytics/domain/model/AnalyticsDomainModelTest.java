package com.jaldishop.backend.analytics.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsDomainModelTest {

    @Test
    @DisplayName("FinancialSummary debe calcular saldo neto y sanitizar redondeo a 2 decimales")
    void shouldCalculateNetBalanceAndSanitizeScale() {
        FinancialSummary summary = FinancialSummary.of(
                new BigDecimal("100.555"),
                5L,
                new BigDecimal("20.100"),
                1L,
                new BigDecimal("100.555"),
                5L,
                BigDecimal.ZERO,
                0L
        );

        assertThat(summary.grossInflowAmount()).isEqualTo(new BigDecimal("100.56"));
        assertThat(summary.settledRefundsAmount()).isEqualTo(new BigDecimal("20.10"));
        assertThat(summary.netFinancialInflow()).isEqualTo(new BigDecimal("80.46"));
        assertThat(summary.orphanPaymentsAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("CommercialSummary debe calcular ticket promedio y proteger división por cero")
    void shouldCalculateAverageTicketAndHandleDivisionByZero() {
        CommercialSummary emptySummary = CommercialSummary.empty();
        assertThat(emptySummary.averageTicket()).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        assertThat(emptySummary.historicalConfirmedOrdersCount()).isZero();
        assertThat(emptySummary.activeOrdersCount()).isZero();

        CommercialSummary activeSummary = CommercialSummary.of(
                5L,
                new BigDecimal("400.00"),
                4L,
                new BigDecimal("320.00"),
                2L,
                new BigDecimal("300.00"),
                new BigDecimal("20.00"),
                new BigDecimal("40.00"),
                1L,
                new BigDecimal("80.00")
        );

        // 400.00 / 5 = 80.00
        assertThat(activeSummary.averageTicket()).isEqualTo(new BigDecimal("80.00"));
        assertThat(activeSummary.historicalConfirmedGmv()).isEqualTo(new BigDecimal("400.00"));
        assertThat(activeSummary.activeCommercialVolume()).isEqualTo(new BigDecimal("320.00"));
        assertThat(activeSummary.cancelledOrdersCount()).isEqualTo(1L);
        assertThat(activeSummary.cancelledOrdersVolume()).isEqualTo(new BigDecimal("80.00"));
    }
}
