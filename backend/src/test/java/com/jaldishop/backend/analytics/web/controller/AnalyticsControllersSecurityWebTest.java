package com.jaldishop.backend.analytics.web.controller;

import com.jaldishop.backend.analytics.domain.model.*;
import com.jaldishop.backend.analytics.domain.port.in.GetAdminPlatformAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.in.GetCustomerAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.in.GetMerchantAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsPdfExportPort;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllersSecurityWebTest {

    @Mock
    private GetCustomerAnalyticsUseCase customerAnalyticsUseCase;

    @Mock
    private GetMerchantAnalyticsUseCase merchantAnalyticsUseCase;

    @Mock
    private GetAdminPlatformAnalyticsUseCase adminAnalyticsUseCase;

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private AnalyticsPdfExportPort analyticsPdfExportPort;

    private MockMvc customerMockMvc;
    private MockMvc merchantMockMvc;
    private MockMvc adminMockMvc;

    private UUID userId;
    private UUID storeId;
    private JwtPrincipal customerPrincipal;
    private JwtPrincipal merchantPrincipal;
    private JwtPrincipal operationalAdminPrincipal;
    private JwtPrincipal primaryAdminPrincipal;

    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        customerPrincipal = new JwtPrincipal(userId, Set.of("CUSTOMER"), 0L, false);
        merchantPrincipal = new JwtPrincipal(userId, Set.of("MERCHANT"), 0L, false);
        operationalAdminPrincipal = new JwtPrincipal(userId, Set.of("ADMIN"), 0L, false);
        primaryAdminPrincipal = new JwtPrincipal(userId, Set.of("ADMIN"), 0L, true);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        CustomerAnalyticsController customerController = new CustomerAnalyticsController(customerAnalyticsUseCase, analyticsPdfExportPort);
        MerchantAnalyticsController merchantController = new MerchantAnalyticsController(merchantAnalyticsUseCase, storeContextService, analyticsPdfExportPort);
        AdminAnalyticsController adminController = new AdminAnalyticsController(adminAnalyticsUseCase, analyticsPdfExportPort);

        customerMockMvc = MockMvcBuilders.standaloneSetup(customerController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        merchantMockMvc = MockMvcBuilders.standaloneSetup(merchantController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminMockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // =========================================================================
    // CUSTOMER ANALYTICS ENDPOINT
    // =========================================================================

    @Test
    @DisplayName("CUSTOMER: Consulta exitosa de sus métricas de compra propias")
    void customer_successfulSummary() throws Exception {
        currentPrincipal = customerPrincipal;
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        CustomerSpendingSummary spending = new CustomerSpendingSummary(
                userId, 2L, new BigDecimal("120.00"), BigDecimal.ZERO, null, null
        );
        CustomerAnalyticsReport report = new CustomerAnalyticsReport(
                userId, period, spending, Collections.emptyList()
        );

        when(customerAnalyticsUseCase.execute(eq(userId), any(PeriodFilter.class))).thenReturn(report);

        customerMockMvc.perform(get("/api/v1/customer/analytics/summary")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.spending.totalOrdersCount").value(2))
                .andExpect(jsonPath("$.spending.totalSpentAmount").value(120.00));

        verify(customerAnalyticsUseCase).execute(eq(userId), any(PeriodFilter.class));
    }

    @Test
    @DisplayName("CUSTOMER: Validación de error cuando rango de fechas supera los 90 días")
    void customer_rangeExceeds90DaysReturns400() throws Exception {
        currentPrincipal = customerPrincipal;

        customerMockMvc.perform(get("/api/v1/customer/analytics/summary")
                        .param("from", "2026-01-01")
                        .param("to", "2026-04-15") // 104 días
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("90 días")));
    }

    @Test
    @DisplayName("CUSTOMER: Validación de error cuando fecha 'from' es posterior a 'to'")
    void customer_invertedDatesReturn400() throws Exception {
        currentPrincipal = customerPrincipal;

        customerMockMvc.perform(get("/api/v1/customer/analytics/summary")
                        .param("from", "2026-10-20")
                        .param("to", "2026-10-10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("no puede ser posterior")));
    }

    // =========================================================================
    // MERCHANT ANALYTICS ENDPOINT
    // =========================================================================

    @Test
    @DisplayName("MERCHANT: Consulta exitosa de métricas de su tienda resuelta por token")
    void merchant_successfulSummary() throws Exception {
        currentPrincipal = merchantPrincipal;
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        when(storeContextService.requireStoreId(merchantPrincipal)).thenReturn(storeId);

        MerchantAnalyticsReport report = new MerchantAnalyticsReport(
                storeId,
                period,
                CommercialSummary.of(10L, new BigDecimal("1000.00"), 9L, new BigDecimal("900.00"), 5L,
                        new BigDecimal("800.00"), new BigDecimal("50.00"), new BigDecimal("150.00"),
                        1L, new BigDecimal("100.00")),
                FinancialSummary.of(new BigDecimal("900.00"), 9L, new BigDecimal("100.00"), 1L,
                        new BigDecimal("900.00"), 9L, BigDecimal.ZERO, 0L),
                Collections.emptyList(),
                CapacityDemandSummary.of(10L, 1L)
        );

        when(merchantAnalyticsUseCase.execute(eq(storeId), any(PeriodFilter.class))).thenReturn(report);

        merchantMockMvc.perform(get("/api/v1/merchant/analytics/summary")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.commercial.historicalConfirmedGmv").value(1000.00))
                .andExpect(jsonPath("$.commercial.activeCommercialVolume").value(900.00))
                .andExpect(jsonPath("$.commercial.cancelledOrdersVolume").value(100.00))
                .andExpect(jsonPath("$.financial.netFinancialInflow").value(800.00));

        verify(storeContextService).requireStoreId(merchantPrincipal);
        verify(merchantAnalyticsUseCase).execute(eq(storeId), any(PeriodFilter.class));
    }

    @Test
    @DisplayName("MERCHANT: Intento de acceso cruzado con storeId ajeno retorna 403 Forbidden")
    void merchant_crossStoreTamperingReturns403() throws Exception {
        currentPrincipal = merchantPrincipal;
        UUID otherStoreId = UUID.randomUUID();

        when(storeContextService.requireStoreId(merchantPrincipal)).thenReturn(storeId);
        doThrow(new AccessDeniedException("No tiene permisos para operar sobre una tienda ajena."))
                .when(storeContextService).validateStoreOwnership(eq(otherStoreId), eq(merchantPrincipal));

        merchantMockMvc.perform(get("/api/v1/merchant/analytics/summary")
                        .param("storeId", otherStoreId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para operar sobre una tienda ajena."));

        verify(merchantAnalyticsUseCase, never()).execute(any(), any());
    }

    // =========================================================================
    // ADMIN ANALYTICS ENDPOINT (ADMIN OPERATIVO Y ADMIN PRINCIPAL)
    // =========================================================================

    @Test
    @DisplayName("ADMIN OPERATIVO: Acceso pleno a reportes globales sin restricción primaryAdmin")
    void operationalAdmin_successfulGlobalSummary() throws Exception {
        currentPrincipal = operationalAdminPrincipal;
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        AdminPlatformAnalyticsReport report = new AdminPlatformAnalyticsReport(
                period,
                null,
                CommercialSummary.of(50L, new BigDecimal("5000.00"), 48L, new BigDecimal("4800.00"), 30L,
                        new BigDecimal("4500.00"), new BigDecimal("200.00"), new BigDecimal("500.00"),
                        2L, new BigDecimal("200.00")),
                FinancialSummary.of(new BigDecimal("5000.00"), 50L, new BigDecimal("200.00"), 2L,
                        new BigDecimal("4800.00"), 48L, new BigDecimal("200.00"), 2L),
                List.of(new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "Torta", "Porción", 20L, new BigDecimal("200.00"))),
                List.of(new StorePerformanceRank(storeId, "Pastelería", 50L, new BigDecimal("5000.00"), new BigDecimal("5000.00"), new BigDecimal("200.00"), new BigDecimal("4800.00"))),
                CapacityDemandSummary.of(50L, 2L)
        );

        when(adminAnalyticsUseCase.execute(any(PeriodFilter.class), isNull())).thenReturn(report);

        adminMockMvc.perform(get("/api/v1/admin/analytics/summary")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commercial.historicalConfirmedGmv").value(5000.00))
                .andExpect(jsonPath("$.financial.orphanPaymentsAmount").value(200.00))
                .andExpect(jsonPath("$.storeRankings[0].storeName").value("Pastelería"))
                .andExpect(jsonPath("$.topProducts[0].productName").value("Torta"));

        verify(adminAnalyticsUseCase).execute(any(PeriodFilter.class), isNull());
    }

    @Test
    @DisplayName("ADMIN PRINCIPAL: Acceso pleno a reportes globales y filtrado por tienda")
    void primaryAdmin_successfulFilteredSummary() throws Exception {
        currentPrincipal = primaryAdminPrincipal;
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        AdminPlatformAnalyticsReport report = new AdminPlatformAnalyticsReport(
                period,
                storeId,
                CommercialSummary.empty(),
                FinancialSummary.empty(),
                Collections.emptyList(),
                Collections.emptyList(),
                CapacityDemandSummary.of(0L, 0L)
        );

        when(adminAnalyticsUseCase.execute(any(PeriodFilter.class), eq(storeId))).thenReturn(report);

        adminMockMvc.perform(get("/api/v1/admin/analytics/summary")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31")
                        .param("storeId", storeId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filterStoreId").value(storeId.toString()))
                .andExpect(jsonPath("$.commercial.historicalConfirmedOrdersCount").value(0));

        verify(adminAnalyticsUseCase).execute(any(PeriodFilter.class), eq(storeId));
    }

    // =========================================================================
    // EXPORTACIÓN PDF ENDPOINTS (FASE B.3)
    // =========================================================================

    @Test
    @DisplayName("CUSTOMER: Exportación PDF exitosa con encabezados seguros")
    void customer_successfulPdfExport() throws Exception {
        currentPrincipal = customerPrincipal;
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        CustomerAnalyticsReport report = new CustomerAnalyticsReport(
                userId, period,
                new CustomerSpendingSummary(userId, 1L, BigDecimal.valueOf(50.00), BigDecimal.ZERO, null, null),
                Collections.emptyList()
        );

        byte[] fakePdfBytes = "%PDF-1.4 Fake customer PDF".getBytes();
        when(customerAnalyticsUseCase.execute(eq(userId), any(PeriodFilter.class))).thenReturn(report);
        when(analyticsPdfExportPort.generateCustomerReportPdf(report)).thenReturn(fakePdfBytes);

        customerMockMvc.perform(get("/api/v1/customer/analytics/export/pdf")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jaldishop-customer-analytics-2026-10-01-al-2026-10-31.pdf\""))
                .andExpect(content().bytes(fakePdfBytes));

        verify(customerAnalyticsUseCase).execute(eq(userId), any(PeriodFilter.class));
        verify(analyticsPdfExportPort).generateCustomerReportPdf(report);
    }

    @Test
    @DisplayName("MERCHANT: Exportación PDF exitosa para la tienda autenticada")
    void merchant_successfulPdfExport() throws Exception {
        currentPrincipal = merchantPrincipal;
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        MerchantAnalyticsReport report = new MerchantAnalyticsReport(
                storeId, period,
                CommercialSummary.empty(),
                FinancialSummary.empty(),
                Collections.emptyList(),
                CapacityDemandSummary.of(0L, 0L)
        );

        byte[] fakePdfBytes = "%PDF-1.4 Fake merchant PDF".getBytes();
        when(storeContextService.requireStoreId(merchantPrincipal)).thenReturn(storeId);
        when(merchantAnalyticsUseCase.execute(eq(storeId), any(PeriodFilter.class))).thenReturn(report);
        when(analyticsPdfExportPort.generateMerchantReportPdf(report)).thenReturn(fakePdfBytes);

        merchantMockMvc.perform(get("/api/v1/merchant/analytics/export/pdf")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jaldishop-merchant-analytics-2026-10-01-al-2026-10-31.pdf\""))
                .andExpect(content().bytes(fakePdfBytes));

        verify(storeContextService).requireStoreId(merchantPrincipal);
        verify(merchantAnalyticsUseCase).execute(eq(storeId), any(PeriodFilter.class));
        verify(analyticsPdfExportPort).generateMerchantReportPdf(report);
    }

    @Test
    @DisplayName("MERCHANT: IDOR en PDF export con storeId ajeno retorna 403 Forbidden")
    void merchant_idorStoreMismatch_pdfExport_returns403() throws Exception {
        currentPrincipal = merchantPrincipal;
        UUID otherStoreId = UUID.randomUUID();

        when(storeContextService.requireStoreId(merchantPrincipal)).thenReturn(storeId);
        doThrow(new AccessDeniedException("No autorizado para acceder a esta tienda."))
                .when(storeContextService).validateStoreOwnership(eq(otherStoreId), eq(merchantPrincipal));

        merchantMockMvc.perform(get("/api/v1/merchant/analytics/export/pdf")
                        .param("storeId", otherStoreId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(storeContextService).validateStoreOwnership(eq(otherStoreId), eq(merchantPrincipal));
        verifyNoInteractions(merchantAnalyticsUseCase);
        verifyNoInteractions(analyticsPdfExportPort);
    }

    @Test
    @DisplayName("ADMIN: Exportación PDF global exitosa")
    void admin_successfulPdfExport() throws Exception {
        currentPrincipal = operationalAdminPrincipal;
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        AdminPlatformAnalyticsReport report = new AdminPlatformAnalyticsReport(
                period, null,
                CommercialSummary.empty(),
                FinancialSummary.empty(),
                Collections.emptyList(),
                Collections.emptyList(),
                CapacityDemandSummary.of(0L, 0L)
        );

        byte[] fakePdfBytes = "%PDF-1.4 Fake admin PDF".getBytes();
        when(adminAnalyticsUseCase.execute(any(PeriodFilter.class), isNull())).thenReturn(report);
        when(analyticsPdfExportPort.generateAdminReportPdf(report)).thenReturn(fakePdfBytes);

        adminMockMvc.perform(get("/api/v1/admin/analytics/export/pdf")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jaldishop-admin-analytics-2026-10-01-al-2026-10-31.pdf\""))
                .andExpect(content().bytes(fakePdfBytes));

        verify(adminAnalyticsUseCase).execute(any(PeriodFilter.class), isNull());
        verify(analyticsPdfExportPort).generateAdminReportPdf(report);
    }
}
