package com.jaldishop.backend.analytics.application.service;

import com.jaldishop.backend.analytics.domain.model.*;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyticsServicesTest {

    private AnalyticsRepository analyticsRepository;
    private GetMerchantAnalyticsService merchantAnalyticsService;
    private GetAdminPlatformAnalyticsService adminAnalyticsService;
    private GetCustomerAnalyticsService customerAnalyticsService;

    @BeforeEach
    void setUp() {
        analyticsRepository = Mockito.mock(AnalyticsRepository.class);
        merchantAnalyticsService = new GetMerchantAnalyticsService(analyticsRepository);
        adminAnalyticsService = new GetAdminPlatformAnalyticsService(analyticsRepository);
        customerAnalyticsService = new GetCustomerAnalyticsService(analyticsRepository);
    }

    @Test
    @DisplayName("Merchant service debe requerir storeId y period no nulos")
    void merchantServiceShouldValidateInputs() {
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertThatThrownBy(() -> merchantAnalyticsService.execute(null, period))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("storeId");

        assertThatThrownBy(() -> merchantAnalyticsService.execute(UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("periodo");
    }

    @Test
    @DisplayName("Merchant service debe ensamblar reporte filtrando por tienda")
    void merchantServiceShouldAssembleReport() {
        UUID storeId = UUID.randomUUID();
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        when(analyticsRepository.findCommercialSummary(eq(period), eq(storeId), isNull()))
                .thenReturn(CommercialSummary.empty());
        when(analyticsRepository.findFinancialSummary(eq(period), eq(storeId), isNull()))
                .thenReturn(FinancialSummary.empty());
        when(analyticsRepository.findTopSellingProducts(eq(period), eq(storeId), isNull(), anyInt()))
                .thenReturn(List.of());
        when(analyticsRepository.findCapacityDemand(eq(period), eq(storeId)))
                .thenReturn(CapacityDemandSummary.of(5, 1));

        MerchantAnalyticsReport report = merchantAnalyticsService.execute(storeId, period);

        assertThat(report).isNotNull();
        assertThat(report.storeId()).isEqualTo(storeId);
        assertThat(report.capacityDemand().committedReservationsCount()).isEqualTo(5);
        verify(analyticsRepository).findCommercialSummary(period, storeId, null);
        verify(analyticsRepository).findFinancialSummary(period, storeId, null);
    }

    @Test
    @DisplayName("Admin platform service debe consultar métricas globales cuando filterStoreId es nulo")
    void adminServiceShouldQueryGlobalMetricsWhenNoStoreFilter() {
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        when(analyticsRepository.findCommercialSummary(eq(period), isNull(), isNull()))
                .thenReturn(CommercialSummary.empty());
        when(analyticsRepository.findFinancialSummary(eq(period), isNull(), isNull()))
                .thenReturn(FinancialSummary.empty());
        when(analyticsRepository.findTopSellingProducts(eq(period), isNull(), isNull(), anyInt()))
                .thenReturn(List.of());
        when(analyticsRepository.findStoreRankings(eq(period), anyInt()))
                .thenReturn(List.of());
        when(analyticsRepository.findCapacityDemand(eq(period), isNull()))
                .thenReturn(CapacityDemandSummary.of(10, 2));

        AdminPlatformAnalyticsReport report = adminAnalyticsService.execute(period, null);

        assertThat(report).isNotNull();
        assertThat(report.filterStoreId()).isNull();
        verify(analyticsRepository).findCommercialSummary(period, null, null);
        verify(analyticsRepository).findStoreRankings(eq(period), eq(20));
    }

    @Test
    @DisplayName("Customer service debe consultar métricas personales por userId")
    void customerServiceShouldQueryPersonalSpending() {
        UUID userId = UUID.randomUUID();
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        when(analyticsRepository.findCustomerSpending(eq(period), eq(userId)))
                .thenReturn(new CustomerSpendingSummary(userId, 3L, new BigDecimal("150.00"), BigDecimal.ZERO, null, null));
        when(analyticsRepository.findTopSellingProducts(eq(period), isNull(), eq(userId), anyInt()))
                .thenReturn(List.of());

        CustomerAnalyticsReport report = customerAnalyticsService.execute(userId, period);

        assertThat(report).isNotNull();
        assertThat(report.userId()).isEqualTo(userId);
        assertThat(report.spending().totalOrdersCount()).isEqualTo(3L);
        assertThat(report.spending().totalSpentAmount()).isEqualTo(new BigDecimal("150.00"));
        verify(analyticsRepository).findCustomerSpending(period, userId);
    }
}
