package com.jaldishop.backend.analytics.web.controller;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CommercialSummary;
import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CustomerSpendingSummary;
import com.jaldishop.backend.analytics.domain.model.FinancialSummary;
import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;
import com.jaldishop.backend.analytics.domain.port.in.GetAdminPlatformAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.in.GetCustomerAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.in.GetMerchantAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsPdfExportPort;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(AnalyticsSecurityEnforcementIntegrationTest.TestSecurityConfig.class)
class AnalyticsSecurityEnforcementIntegrationTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean GetCustomerAnalyticsUseCase customerUseCase() { return mock(GetCustomerAnalyticsUseCase.class); }
        @Bean GetMerchantAnalyticsUseCase merchantUseCase() { return mock(GetMerchantAnalyticsUseCase.class); }
        @Bean GetAdminPlatformAnalyticsUseCase adminUseCase() { return mock(GetAdminPlatformAnalyticsUseCase.class); }
        @Bean StoreContextService storeContextService() { return mock(StoreContextService.class); }
        @Bean AnalyticsPdfExportPort analyticsPdfExportPort() { return mock(AnalyticsPdfExportPort.class); }

        @Bean CustomerAnalyticsController customerController(GetCustomerAnalyticsUseCase useCase, AnalyticsPdfExportPort pdfPort) {
            return new CustomerAnalyticsController(useCase, pdfPort);
        }

        @Bean MerchantAnalyticsController merchantController(GetMerchantAnalyticsUseCase useCase, StoreContextService storeService, AnalyticsPdfExportPort pdfPort) {
            return new MerchantAnalyticsController(useCase, storeService, pdfPort);
        }

        @Bean AdminAnalyticsController adminController(GetAdminPlatformAnalyticsUseCase useCase, AnalyticsPdfExportPort pdfPort) {
            return new AdminAnalyticsController(useCase, pdfPort);
        }
    }

    @Autowired private CustomerAnalyticsController customerController;
    @Autowired private MerchantAnalyticsController merchantController;
    @Autowired private AdminAnalyticsController adminController;

    @Autowired private GetCustomerAnalyticsUseCase customerUseCase;
    @Autowired private GetMerchantAnalyticsUseCase merchantUseCase;
    @Autowired private GetAdminPlatformAnalyticsUseCase adminUseCase;
    @Autowired private StoreContextService storeContextService;
    @Autowired private AnalyticsPdfExportPort analyticsPdfExportPort;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();

    // =========================================================================
    // CASOS SIN AUTENTICACIÓN
    // =========================================================================

    @Test
    @DisplayName("Sin autenticación: Customer endpoint deniega acceso")
    void unauthenticatedCustomerThrows() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                customerController.getSummary(LocalDate.now(), LocalDate.now(), null)
        );
    }

    @Test
    @DisplayName("Sin autenticación: Merchant endpoint deniega acceso")
    void unauthenticatedMerchantThrows() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                merchantController.getSummary(LocalDate.now(), LocalDate.now(), null, null)
        );
    }

    @Test
    @DisplayName("Sin autenticación: Admin endpoint deniega acceso")
    void unauthenticatedAdminThrows() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                adminController.getSummary(LocalDate.now(), LocalDate.now(), null, null)
        );
    }

    // =========================================================================
    // CONTROL DE ACCESO POR ROL CRUZADO (403 FORBIDDEN)
    // =========================================================================

    @Test
    @WithMockUser(roles = "MERCHANT")
    @DisplayName("Rol MERCHANT no puede acceder a endpoint de CUSTOMER (403)")
    void merchantCannotAccessCustomerEndpoint() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("MERCHANT"), 0L, false);
        assertThrows(AccessDeniedException.class, () ->
                customerController.getSummary(LocalDate.now(), LocalDate.now(), principal)
        );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Rol CUSTOMER no puede acceder a endpoint de MERCHANT (403)")
    void customerCannotAccessMerchantEndpoint() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("CUSTOMER"), 0L, false);
        assertThrows(AccessDeniedException.class, () ->
                merchantController.getSummary(LocalDate.now(), LocalDate.now(), null, principal)
        );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Rol CUSTOMER no puede acceder a endpoint global de ADMIN (403)")
    void customerCannotAccessAdminEndpoint() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("CUSTOMER"), 0L, false);
        assertThrows(AccessDeniedException.class, () ->
                adminController.getSummary(LocalDate.now(), LocalDate.now(), null, principal)
        );
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    @DisplayName("Rol MERCHANT no puede acceder a endpoint global de ADMIN (403)")
    void merchantCannotAccessAdminEndpoint() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("MERCHANT"), 0L, false);
        assertThrows(AccessDeniedException.class, () ->
                adminController.getSummary(LocalDate.now(), LocalDate.now(), null, principal)
        );
    }

    // =========================================================================
    // ACCESO CON ROLES CORRECTOS
    // =========================================================================

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Rol CUSTOMER accede exitosamente a su endpoint")
    void customerAuthorized() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("CUSTOMER"), 0L, false);
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        when(customerUseCase.getCustomerAnalytics(eq(USER_ID), any())).thenReturn(
                new CustomerAnalyticsReport(USER_ID, period, new CustomerSpendingSummary(USER_ID, 1L, BigDecimal.TEN, BigDecimal.ZERO, null, null), Collections.emptyList())
        );

        assertDoesNotThrow(() ->
                customerController.getSummary(period.fromDate(), period.toDate(), principal)
        );
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    @DisplayName("Rol MERCHANT accede exitosamente a su tienda")
    void merchantAuthorized() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("MERCHANT"), 0L, false);
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        when(storeContextService.requireStoreId(principal)).thenReturn(STORE_ID);
        when(merchantUseCase.getMerchantAnalytics(eq(STORE_ID), any())).thenReturn(
                new MerchantAnalyticsReport(STORE_ID, period, CommercialSummary.empty(), FinancialSummary.empty(), Collections.emptyList(), null)
        );

        assertDoesNotThrow(() ->
                merchantController.getSummary(period.fromDate(), period.toDate(), null, principal)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN OPERATIVO (primaryAdmin = false): Acceso concedido a reportes globales")
    void operationalAdminAuthorized() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("ADMIN"), 0L, false);
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        when(adminUseCase.getPlatformAnalytics(any(), any())).thenReturn(
                new AdminPlatformAnalyticsReport(period, null, CommercialSummary.empty(), FinancialSummary.empty(), Collections.emptyList(), Collections.emptyList(), null)
        );

        assertDoesNotThrow(() ->
                adminController.getSummary(period.fromDate(), period.toDate(), null, principal)
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN PRINCIPAL (primaryAdmin = true): Acceso concedido a reportes globales")
    void primaryAdminAuthorized() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("ADMIN"), 0L, true);
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        when(adminUseCase.getPlatformAnalytics(any(), any())).thenReturn(
                new AdminPlatformAnalyticsReport(period, null, CommercialSummary.empty(), FinancialSummary.empty(), Collections.emptyList(), Collections.emptyList(), null)
        );

        assertDoesNotThrow(() ->
                adminController.getSummary(period.fromDate(), period.toDate(), null, principal)
        );
    }

    // =========================================================================
    // SEGURIDAD EN EXPORTACIÓN PDF (FASE B.3)
    // =========================================================================

    @Test
    @DisplayName("Sin autenticación: Customer export/pdf lanza excepción de credenciales")
    void unauthenticatedCustomerExportPdfDenied() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("CUSTOMER"), 0L, false);
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                customerController.exportPdf(LocalDate.now(), LocalDate.now(), principal)
        );
    }

    @Test
    @DisplayName("Sin autenticación: Merchant export/pdf lanza excepción de credenciales")
    void unauthenticatedMerchantExportPdfDenied() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("MERCHANT"), 0L, false);
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                merchantController.exportPdf(LocalDate.now(), LocalDate.now(), null, principal)
        );
    }

    @Test
    @DisplayName("Sin autenticación: Admin export/pdf lanza excepción de credenciales")
    void unauthenticatedAdminExportPdfDenied() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("ADMIN"), 0L, false);
        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                adminController.exportPdf(LocalDate.now(), LocalDate.now(), null, principal)
        );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Rol CUSTOMER intentando export/pdf de MERCHANT es denegado con 403 AccessDenied")
    void customerAttemptingMerchantExportPdfDenied() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("CUSTOMER"), 0L, false);
        assertThrows(AccessDeniedException.class, () ->
                merchantController.exportPdf(LocalDate.now(), LocalDate.now(), null, principal)
        );
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    @DisplayName("Rol MERCHANT intentando export/pdf de ADMIN es denegado con 403 AccessDenied")
    void merchantAttemptingAdminExportPdfDenied() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("MERCHANT"), 0L, false);
        assertThrows(AccessDeniedException.class, () ->
                adminController.exportPdf(LocalDate.now(), LocalDate.now(), null, principal)
        );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Rol CUSTOMER: Exportación PDF autorizada con su propio rol")
    void customerAuthorizedPdfExport() {
        JwtPrincipal principal = new JwtPrincipal(USER_ID, Set.of("CUSTOMER"), 0L, false);
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        when(customerUseCase.getCustomerAnalytics(eq(USER_ID), any())).thenReturn(
                new CustomerAnalyticsReport(USER_ID, period, new CustomerSpendingSummary(USER_ID, 0L, BigDecimal.ZERO, BigDecimal.ZERO, null, null), Collections.emptyList())
        );
        when(analyticsPdfExportPort.generateCustomerReportPdf(any())).thenReturn("%PDF-1.4 mock".getBytes());

        assertDoesNotThrow(() ->
                customerController.exportPdf(period.fromDate(), period.toDate(), principal)
        );
    }
}
