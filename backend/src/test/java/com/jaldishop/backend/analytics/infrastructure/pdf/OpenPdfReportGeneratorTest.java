package com.jaldishop.backend.analytics.infrastructure.pdf;

import com.jaldishop.backend.analytics.domain.model.AdminPlatformAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CapacityDemandSummary;
import com.jaldishop.backend.analytics.domain.model.CommercialSummary;
import com.jaldishop.backend.analytics.domain.model.CustomerAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.CustomerSpendingSummary;
import com.jaldishop.backend.analytics.domain.model.FinancialSummary;
import com.jaldishop.backend.analytics.domain.model.MerchantAnalyticsReport;
import com.jaldishop.backend.analytics.domain.model.PeriodFilter;
import com.jaldishop.backend.analytics.domain.model.ProductSalesRank;
import com.jaldishop.backend.analytics.domain.model.StorePerformanceRank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenPdfReportGeneratorTest {

    private OpenPdfReportGeneratorAdapter generator;

    @BeforeEach
    void setUp() {
        generator = new OpenPdfReportGeneratorAdapter();
    }

    private void assertValidPdf(byte[] pdfBytes) {
        assertNotNull(pdfBytes, "PDF bytes no deben ser nulos");
        assertTrue(pdfBytes.length > 500, "PDF debe contener un tamaño razonable (>500 bytes)");
        String header = new String(pdfBytes, 0, Math.min(10, pdfBytes.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"), "El documento debe comenzar con la firma canónica %PDF-");
    }

    @Test
    @DisplayName("CUSTOMER: Generación exitosa de PDF con compras y productos")
    void shouldGenerateCustomerReportPdfSuccessfully() {
        UUID customerId = UUID.randomUUID();
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        CustomerSpendingSummary spending = new CustomerSpendingSummary(
                customerId,
                5L,
                BigDecimal.valueOf(450.50),
                BigDecimal.valueOf(25.00),
                Instant.now().minusSeconds(86400 * 10),
                Instant.now()
        );

        List<ProductSalesRank> products = List.of(
                new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "Arroz con Pollo", "Personal", 3L, BigDecimal.valueOf(90.00)),
                new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "Ceviche Mixto", "Familiar", 2L, BigDecimal.valueOf(120.00))
        );

        CustomerAnalyticsReport report = new CustomerAnalyticsReport(customerId, period, spending, products);

        byte[] pdf = generator.generateCustomerReportPdf(report);
        assertValidPdf(pdf);
    }

    @Test
    @DisplayName("CUSTOMER: Generación exitosa de PDF en periodo vacío (sin compras)")
    void shouldGenerateCustomerReportPdfWhenEmpty() {
        UUID customerId = UUID.randomUUID();
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));

        CustomerSpendingSummary spending = new CustomerSpendingSummary(
                customerId, 0L, BigDecimal.ZERO, BigDecimal.ZERO, null, null
        );

        CustomerAnalyticsReport report = new CustomerAnalyticsReport(customerId, period, spending, List.of());

        byte[] pdf = generator.generateCustomerReportPdf(report);
        assertValidPdf(pdf);
    }

    @Test
    @DisplayName("MERCHANT: Generación exitosa de PDF con actividad comercial y financiera completa")
    void shouldGenerateMerchantReportPdfSuccessfully() {
        UUID storeId = UUID.randomUUID();
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        CommercialSummary commercial = new CommercialSummary(
                120L, BigDecimal.valueOf(15400.00),
                115L, BigDecimal.valueOf(14800.00),
                110L, BigDecimal.valueOf(14000.00),
                BigDecimal.valueOf(200.00), BigDecimal.valueOf(1000.00),
                5L, BigDecimal.valueOf(600.00),
                BigDecimal.valueOf(128.33)
        );

        FinancialSummary financial = FinancialSummary.of(
                BigDecimal.valueOf(15000.00), 120L,
                BigDecimal.valueOf(500.00), 3L,
                BigDecimal.valueOf(15000.00), 120L,
                BigDecimal.ZERO, 0L
        );

        CapacityDemandSummary capacity = CapacityDemandSummary.of(45L, 2L);

        List<ProductSalesRank> topProducts = List.of(
                new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "Lomo Saltado", "Especial", 40L, BigDecimal.valueOf(1800.00)),
                new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "Ají de Gallina", "Estándar", 30L, BigDecimal.valueOf(900.00))
        );

        MerchantAnalyticsReport report = new MerchantAnalyticsReport(
                storeId, period, commercial, financial, topProducts, capacity
        );

        byte[] pdf = generator.generateMerchantReportPdf(report);
        assertValidPdf(pdf);
    }

    @Test
    @DisplayName("MERCHANT: Manejo multi-página con decenas de productos sin desbordamiento ni fallos")
    void shouldHandleMultiPageMerchantReportGracefully() {
        UUID storeId = UUID.randomUUID();
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 30));

        CommercialSummary commercial = new CommercialSummary(
                500L, BigDecimal.valueOf(50000.00),
                490L, BigDecimal.valueOf(49000.00),
                480L, BigDecimal.valueOf(48000.00),
                BigDecimal.ZERO, BigDecimal.ZERO,
                10L, BigDecimal.valueOf(1000.00),
                BigDecimal.valueOf(100.00)
        );
        FinancialSummary financial = FinancialSummary.of(
                BigDecimal.valueOf(49000.00), 490L,
                BigDecimal.valueOf(1000.00), 10L,
                BigDecimal.valueOf(49000.00), 490L,
                BigDecimal.ZERO, 0L
        );

        List<ProductSalesRank> manyProducts = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            manyProducts.add(new ProductSalesRank(
                    UUID.randomUUID(), UUID.randomUUID(),
                    "Producto Menú #" + i, "Variante Tamaño " + (i % 3),
                    i * 5L, BigDecimal.valueOf(i * 50.0)
            ));
        }

        MerchantAnalyticsReport report = new MerchantAnalyticsReport(
                storeId, period, commercial, financial, manyProducts, CapacityDemandSummary.of(100L, 5L)
        );

        byte[] pdf = generator.generateMerchantReportPdf(report);
        assertValidPdf(pdf);
        // Debe ser un documento más grande pero acotado (<200KB)
        assertTrue(pdf.length < 200 * 1024, "El PDF no debe exceder un límite de memoria prudente (200KB)");
    }

    @Test
    @DisplayName("ADMIN: Generación exitosa de PDF global con rankings de tiendas y pagos huérfanos")
    void shouldGenerateAdminPlatformReportPdfSuccessfully() {
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        CommercialSummary commercial = new CommercialSummary(
                350L, BigDecimal.valueOf(42000.00),
                340L, BigDecimal.valueOf(40500.00),
                330L, BigDecimal.valueOf(39000.00),
                BigDecimal.valueOf(500.00), BigDecimal.valueOf(2000.00),
                10L, BigDecimal.valueOf(1500.00),
                BigDecimal.valueOf(120.00)
        );

        FinancialSummary financial = FinancialSummary.of(
                BigDecimal.valueOf(41000.00), 345L,
                BigDecimal.valueOf(1200.00), 8L,
                BigDecimal.valueOf(40000.00), 340L,
                BigDecimal.valueOf(1000.00), 5L // Pagos huérfanos
        );

        List<StorePerformanceRank> storeRankings = List.of(
                new StorePerformanceRank(UUID.randomUUID(), "Picantería Doña Luz", 180L, BigDecimal.valueOf(22000.00), BigDecimal.valueOf(22000.00), BigDecimal.ZERO, BigDecimal.valueOf(22000.00)),
                new StorePerformanceRank(UUID.randomUUID(), "Pollería El Rey", 170L, BigDecimal.valueOf(20000.00), BigDecimal.valueOf(19000.00), BigDecimal.valueOf(1000.00), BigDecimal.valueOf(18000.00))
        );

        List<ProductSalesRank> topProducts = List.of(
                new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "1/4 Pollo a la Brasa", "Papas y Ensalada", 120L, BigDecimal.valueOf(3600.00))
        );

        AdminPlatformAnalyticsReport report = new AdminPlatformAnalyticsReport(
                period, null, commercial, financial, topProducts, storeRankings, CapacityDemandSummary.of(80L, 4L)
        );

        byte[] pdf = generator.generateAdminReportPdf(report);
        assertValidPdf(pdf);
    }

    @Test
    @DisplayName("Caracteres especiales y moneda PEN en español se procesan sin excepciones")
    void shouldHandleSpanishCharactersAndPenCurrencyWithoutErrors() {
        UUID storeId = UUID.randomUUID();
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        List<ProductSalesRank> specialProducts = List.of(
                new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "Ceviche Piurano con Ají Limo & Choclo", "Porción Extra Grande", 10L, BigDecimal.valueOf(450.00)),
                new ProductSalesRank(UUID.randomUUID(), UUID.randomUUID(), "Ñoquis a la Huancaína con Lomo", "Fusión Especial", 8L, BigDecimal.valueOf(380.00))
        );

        MerchantAnalyticsReport report = new MerchantAnalyticsReport(
                storeId, period,
                new CommercialSummary(18L, BigDecimal.valueOf(830.00), 18L, BigDecimal.valueOf(830.00), 18L, BigDecimal.valueOf(830.00), BigDecimal.ZERO, BigDecimal.ZERO, 0L, BigDecimal.ZERO, BigDecimal.valueOf(46.11)),
                FinancialSummary.of(BigDecimal.valueOf(830.00), 18L, BigDecimal.ZERO, 0L, BigDecimal.valueOf(830.00), 18L, BigDecimal.ZERO, 0L),
                specialProducts,
                CapacityDemandSummary.of(5L, 0L)
        );

        byte[] pdf = assertDoesNotThrow(() -> generator.generateMerchantReportPdf(report));
        assertValidPdf(pdf);
    }
}
