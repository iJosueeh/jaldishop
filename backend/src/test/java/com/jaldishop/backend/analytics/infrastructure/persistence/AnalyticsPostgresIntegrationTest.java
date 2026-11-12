package com.jaldishop.backend.analytics.infrastructure.persistence;

import com.jaldishop.backend.analytics.domain.model.*;
import com.jaldishop.backend.analytics.domain.port.in.GetAdminPlatformAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.in.GetCustomerAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.in.GetMerchantAnalyticsUseCase;
import com.jaldishop.backend.analytics.domain.port.out.AnalyticsPdfExportPort;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AnalyticsPostgresIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private static final UUID STORE_A = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID STORE_B = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e6");
    private static final UUID USER_ID_1 = UUID.randomUUID();
    private static final UUID USER_ID_2 = UUID.randomUUID();

    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdVariantIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdUserIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdStoreIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.hikari.jdbc-url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.hikari.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.hikari.password", database::password);
        registry.add("spring.flyway.url", database::url);
        registry.add("spring.flyway.user", database::username);
        registry.add("spring.flyway.password", database::password);
        registry.add("spring.flyway.validate-on-migrate", () -> "false");
    }

    @BeforeAll
    static void initSchema() {
        Flyway.configure()
                .dataSource(database.url(), database.username(), database.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(false)
                .load()
                .migrate();
    }

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private AnalyticsPdfExportPort analyticsPdfExportPort;

    @Autowired
    private GetCustomerAnalyticsUseCase getCustomerAnalyticsUseCase;

    @Autowired
    private GetMerchantAnalyticsUseCase getMerchantAnalyticsUseCase;

    @Autowired
    private GetAdminPlatformAnalyticsUseCase getAdminPlatformAnalyticsUseCase;

    private JdbcAnalyticsRepositoryAdapter repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcAnalyticsRepositoryAdapter(jdbcTemplate);
    }

    @AfterAll
    static void cleanUpTestData() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            for (UUID orderId : createdOrderIds) {
                deleteById(conn, "DELETE FROM order_status_history WHERE order_id = ?", orderId);
                deleteById(conn, "DELETE FROM order_items WHERE order_id = ?", orderId);
                deleteById(conn, "DELETE FROM orders WHERE id = ?", orderId);
            }

            for (UUID paymentId : createdPaymentIds) {
                deleteById(conn, "DELETE FROM payment_refund_execution_attempts WHERE refund_intent_id IN (SELECT id FROM payment_refund_intents WHERE payment_id = ?)", paymentId);
                deleteById(conn, "DELETE FROM payment_refund_intents WHERE payment_id = ?", paymentId);
                deleteById(conn, "DELETE FROM payment_attempts WHERE payment_id = ?", paymentId);
                deleteById(conn, "DELETE FROM payments WHERE id = ?", paymentId);
            }

            for (UUID capResId : createdCapResIds) {
                deleteById(conn, "DELETE FROM capacity_reservations WHERE id = ?", capResId);
            }

            for (UUID variantId : createdVariantIds) {
                deleteById(conn, "DELETE FROM product_variants WHERE id = ?", variantId);
            }

            for (UUID userId : createdUserIds) {
                deleteById(conn, "DELETE FROM user_roles WHERE user_id = ?", userId);
                deleteById(conn, "DELETE FROM users WHERE id = ?", userId);
            }

            for (UUID storeId : createdStoreIds) {
                deleteById(conn, "DELETE FROM stores WHERE id = ?", storeId);
            }
        }
    }

    private static void deleteById(Connection conn, String sql, UUID id) {
        try (var ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    // =========================================================================
    // PRUEBAS DE INTEGRACIÓN: 11 ESCENARIOS OBLIGATORIOS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Pago aprobado en octubre y reembolsado en noviembre: imputación contable independiente")
    void scenario01_paymentOctRefundNov() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime octDate = ZonedDateTime.of(2026, 10, 15, 14, 0, 0, 0, LIMA);
        ZonedDateTime novDate = ZonedDateTime.of(2026, 11, 3, 10, 0, 0, 0, LIMA);

        UUID capResId = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 15), "COMMITTED");
        UUID paymentId = insertPayment(capResId, new BigDecimal("100.00"), "APPROVED", octDate.toInstant(),
                "COMPLETED", new BigDecimal("100.00"), novDate.toInstant());
        insertOrder(STORE_A, USER_ID_1, paymentId, capResId, "CONFIRMED", new BigDecimal("100.00"), octDate.toInstant());

        PeriodFilter periodOct = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        FinancialSummary finOct = repository.findFinancialSummary(periodOct, STORE_A, null);
        assertThat(finOct.grossInflowAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(finOct.settledRefundsAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(finOct.netFinancialInflow()).isEqualTo(new BigDecimal("100.00"));

        PeriodFilter periodNov = PeriodFilter.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
        FinancialSummary finNov = repository.findFinancialSummary(periodNov, STORE_A, null);
        assertThat(finNov.grossInflowAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(finNov.settledRefundsAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(finNov.netFinancialInflow()).isEqualTo(new BigDecimal("-100.00"));
    }

    @Test
    @Order(2)
    @DisplayName("2. Pago aprobado y reembolsado en el mismo periodo (noviembre): saldo neto se compensa")
    void scenario02_paymentAndRefundInSameMonth() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime payDate = ZonedDateTime.of(2026, 11, 5, 11, 0, 0, 0, LIMA);
        ZonedDateTime refDate = ZonedDateTime.of(2026, 11, 10, 16, 0, 0, 0, LIMA);

        UUID capResId = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 11, 5), "COMMITTED");
        UUID paymentId = insertPayment(capResId, new BigDecimal("80.00"), "APPROVED", payDate.toInstant(),
                "COMPLETED", new BigDecimal("80.00"), refDate.toInstant());
        insertOrder(STORE_A, USER_ID_1, paymentId, capResId, "CONFIRMED", new BigDecimal("80.00"), payDate.toInstant());

        PeriodFilter periodNov = PeriodFilter.of(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12));
        FinancialSummary fin = repository.findFinancialSummary(periodNov, STORE_A, null);

        assertThat(fin.grossInflowAmount()).isEqualTo(new BigDecimal("80.00"));
        assertThat(fin.settledRefundsAmount()).isEqualTo(new BigDecimal("80.00"));
        assertThat(fin.netFinancialInflow()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @Order(3)
    @DisplayName("3. Pago aprobado sin pedido (huérfano): pasivo en custodia para admin, cero ventas para comerciante")
    void scenario03_orphanApprovedPayment() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime octDate = ZonedDateTime.of(2026, 10, 20, 15, 30, 0, 0, LIMA);
        UUID capResId = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 20), "ACTIVE");
        // Pago aprobado SIN insertar orden asociada
        insertPayment(capResId, new BigDecimal("50.00"), "APPROVED", octDate.toInstant(),
                "NOT_REQUIRED", null, null);

        PeriodFilter periodOct = PeriodFilter.of(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 21));

        // Vista ADMIN GLOBAL (storeId = null): reporta huérfanos en custodia
        FinancialSummary adminFin = repository.findFinancialSummary(periodOct, null, null);
        assertThat(adminFin.orphanPaymentsAmount()).isGreaterThanOrEqualTo(new BigDecimal("50.00"));
        assertThat(adminFin.orphanPaymentsCount()).isGreaterThanOrEqualTo(1L);

        // Vista MERCHANT (storeId = STORE_A): NUNCA se acredita como venta comercial
        FinancialSummary merchantFin = repository.findFinancialSummary(periodOct, STORE_A, null);
        assertThat(merchantFin.grossInflowAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(merchantFin.orphanPaymentsAmount()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Pedido cancelado con reembolso PENDING: cancelación comercial registrada, reembolso efectivo cero")
    void scenario04_cancelledOrderWithPendingRefund() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime orderDate = ZonedDateTime.of(2026, 10, 10, 12, 0, 0, 0, LIMA);
        ZonedDateTime cancelDate = ZonedDateTime.of(2026, 10, 25, 14, 0, 0, 0, LIMA);

        UUID capResId = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 10), "RELEASED");
        UUID paymentId = insertPayment(capResId, new BigDecimal("120.00"), "APPROVED", orderDate.toInstant(),
                "PENDING", new BigDecimal("120.00"), null);
        UUID orderId = insertOrder(STORE_A, USER_ID_1, paymentId, capResId, "CANCELLED", new BigDecimal("120.00"), orderDate.toInstant());
        insertStatusHistory(orderId, USER_ID_1, "CANCELLED", cancelDate.toInstant());

        PeriodFilter periodCancel = PeriodFilter.of(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 26));

        CommercialSummary comm = repository.findCommercialSummary(periodCancel, STORE_A, null);
        assertThat(comm.cancelledOrdersCount()).isEqualTo(1L);
        assertThat(comm.cancelledOrdersVolume()).isEqualTo(new BigDecimal("120.00"));

        FinancialSummary fin = repository.findFinancialSummary(periodCancel, STORE_A, null);
        assertThat(fin.settledRefundsAmount()).isEqualTo(new BigDecimal("0.00")); // PENDING no es dinero restituido
    }

    @Test
    @Order(5)
    @DisplayName("5. Pedido cancelado con reembolso COMPLETED: ambas métricas coinciden en periodo")
    void scenario05_cancelledOrderWithCompletedRefund() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime orderDate = ZonedDateTime.of(2026, 10, 12, 12, 0, 0, 0, LIMA);
        ZonedDateTime cancelDate = ZonedDateTime.of(2026, 10, 26, 18, 0, 0, 0, LIMA);

        UUID capResId = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 12), "RELEASED");
        UUID paymentId = insertPayment(capResId, new BigDecimal("90.00"), "APPROVED", orderDate.toInstant(),
                "COMPLETED", new BigDecimal("90.00"), cancelDate.toInstant());
        UUID orderId = insertOrder(STORE_A, USER_ID_1, paymentId, capResId, "CANCELLED", new BigDecimal("90.00"), orderDate.toInstant());
        insertStatusHistory(orderId, USER_ID_1, "CANCELLED", cancelDate.toInstant());

        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 10, 27));

        CommercialSummary comm = repository.findCommercialSummary(period, STORE_A, null);
        assertThat(comm.cancelledOrdersCount()).isEqualTo(1L);
        assertThat(comm.cancelledOrdersVolume()).isEqualTo(new BigDecimal("90.00"));

        FinancialSummary fin = repository.findFinancialSummary(period, STORE_A, null);
        assertThat(fin.settledRefundsAmount()).isEqualTo(new BigDecimal("90.00"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Reintentos técnicos de reembolso en BD: no duplican importes financieros")
    void scenario06_technicalRefundRetriesDoNotDuplicate() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime novDate = ZonedDateTime.of(2026, 11, 15, 10, 0, 0, 0, LIMA);
        UUID capResId = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 11, 15), "RELEASED");
        UUID paymentId = insertPayment(capResId, new BigDecimal("75.00"), "APPROVED", novDate.toInstant(),
                "COMPLETED", new BigDecimal("75.00"), novDate.toInstant());
        insertOrder(STORE_A, USER_ID_1, paymentId, capResId, "CANCELLED", new BigDecimal("75.00"), novDate.toInstant());

        // Insertar 1 intent y 3 execution attempts técnicos HTTP
        UUID intentId = UUID.randomUUID();
        insertRefundIntent(intentId, paymentId, new BigDecimal("75.00"), novDate.toInstant());
        insertExecutionAttempt(intentId, 1, "FAILED", novDate.toInstant());
        insertExecutionAttempt(intentId, 2, "FAILED", novDate.toInstant());
        insertExecutionAttempt(intentId, 3, "SUCCESSFUL", novDate.toInstant());

        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 11, 15), LocalDate.of(2026, 11, 15));
        FinancialSummary fin = repository.findFinancialSummary(period, STORE_A, null);

        // Debe sumar exactamente 75.00 una sola vez, nunca 225.00
        assertThat(fin.settledRefundsAmount()).isEqualTo(new BigDecimal("75.00"));
        assertThat(fin.settledRefundsCount()).isEqualTo(1L);
    }

    @Test
    @Order(7)
    @DisplayName("7. Dos tiendas con operaciones independientes: estricto aislamiento multitenant")
    void scenario07_twoStoresIsolation() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Tienda Alfa");
        ensureUserAndStore(USER_ID_2, STORE_B, "Tienda Beta");

        ZonedDateTime testDate = ZonedDateTime.of(2026, 10, 5, 12, 0, 0, 0, LIMA);

        UUID capA = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 5), "COMMITTED");
        UUID payA = insertPayment(capA, new BigDecimal("200.00"), "APPROVED", testDate.toInstant(), "NOT_REQUIRED", null, null);
        insertOrder(STORE_A, USER_ID_1, payA, capA, "CONFIRMED", new BigDecimal("200.00"), testDate.toInstant());

        UUID capB = insertCapacityReservation(STORE_B, USER_ID_2, LocalDate.of(2026, 10, 5), "COMMITTED");
        UUID payB = insertPayment(capB, new BigDecimal("300.00"), "APPROVED", testDate.toInstant(), "NOT_REQUIRED", null, null);
        insertOrder(STORE_B, USER_ID_2, payB, capB, "CONFIRMED", new BigDecimal("300.00"), testDate.toInstant());

        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 5));

        CommercialSummary commA = repository.findCommercialSummary(period, STORE_A, null);
        CommercialSummary commB = repository.findCommercialSummary(period, STORE_B, null);

        assertThat(commA.historicalConfirmedGmv()).isEqualTo(new BigDecimal("200.00"));
        assertThat(commB.historicalConfirmedGmv()).isEqualTo(new BigDecimal("300.00"));
    }

    @Test
    @Order(8)
    @DisplayName("8. Fechas límite de filtros: intervalo semiabierto [start, end) exacto")
    void scenario08_filterBoundaries() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        // Evento 1: exactamente en el inicio del día 2026-10-01T00:00:00-05:00
        ZonedDateTime startEvent = ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, LIMA);
        UUID cap1 = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 1), "COMMITTED");
        UUID pay1 = insertPayment(cap1, new BigDecimal("40.00"), "APPROVED", startEvent.toInstant(), "NOT_REQUIRED", null, null);
        insertOrder(STORE_A, USER_ID_1, pay1, cap1, "CONFIRMED", new BigDecimal("40.00"), startEvent.toInstant());

        // Evento 2: en el último segundo del día 2026-10-01T23:59:59-05:00
        ZonedDateTime endEvent = ZonedDateTime.of(2026, 10, 1, 23, 59, 59, 0, LIMA);
        UUID cap2 = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 1), "COMMITTED");
        UUID pay2 = insertPayment(cap2, new BigDecimal("60.00"), "APPROVED", endEvent.toInstant(), "NOT_REQUIRED", null, null);
        insertOrder(STORE_A, USER_ID_1, pay2, cap2, "CONFIRMED", new BigDecimal("60.00"), endEvent.toInstant());

        // Evento 3: inicio del día siguiente 2026-10-02T00:00:00-05:00 (debe quedar EXCLUIDO de 2026-10-01)
        ZonedDateTime nextDayEvent = ZonedDateTime.of(2026, 10, 2, 0, 0, 0, 0, LIMA);
        UUID cap3 = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 10, 2), "COMMITTED");
        UUID pay3 = insertPayment(cap3, new BigDecimal("100.00"), "APPROVED", nextDayEvent.toInstant(), "NOT_REQUIRED", null, null);
        insertOrder(STORE_A, USER_ID_1, pay3, cap3, "CONFIRMED", new BigDecimal("100.00"), nextDayEvent.toInstant());

        PeriodFilter filterSingleDay = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1));
        CommercialSummary comm = repository.findCommercialSummary(filterSingleDay, STORE_A, null);

        // Debe incluir evento 1 y evento 2 (40 + 60 = 100.00), excluyendo el evento 3
        assertThat(comm.historicalConfirmedGmv()).isEqualTo(new BigDecimal("100.00"));
        assertThat(comm.historicalConfirmedOrdersCount()).isEqualTo(2L);
    }

    @Test
    @Order(9)
    @DisplayName("9. Periodos vacíos: retorna estructura limpia con ceros sin excepción")
    void scenario09_emptyPeriodReturnsCleanZeros() {
        PeriodFilter emptyPeriod = PeriodFilter.of(LocalDate.of(2029, 1, 1), LocalDate.of(2029, 1, 15));

        CommercialSummary comm = repository.findCommercialSummary(emptyPeriod, STORE_A, null);
        FinancialSummary fin = repository.findFinancialSummary(emptyPeriod, STORE_A, null);
        List<ProductSalesRank> products = repository.findTopSellingProducts(emptyPeriod, STORE_A, null, 10);
        CapacityDemandSummary capacity = repository.findCapacityDemand(emptyPeriod, STORE_A);

        assertThat(comm.historicalConfirmedOrdersCount()).isZero();
        assertThat(comm.historicalConfirmedGmv()).isEqualTo(new BigDecimal("0.00"));
        assertThat(comm.averageTicket()).isEqualTo(new BigDecimal("0.00"));
        assertThat(fin.grossInflowAmount()).isEqualTo(new BigDecimal("0.00"));
        assertThat(fin.netFinancialInflow()).isEqualTo(new BigDecimal("0.00"));
        assertThat(products).isEmpty();
        assertThat(capacity.committedReservationsCount()).isZero();
    }

    @Test
    @Order(10)
    @DisplayName("10. Reembolsos parciales: calcula saldo financiero remanente conforme al modelo")
    void scenario10_partialRefundCompatibility() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime decDate = ZonedDateTime.of(2026, 12, 1, 10, 0, 0, 0, LIMA);
        UUID capResId = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 12, 1), "COMMITTED");
        UUID paymentId = insertPayment(capResId, new BigDecimal("200.00"), "APPROVED", decDate.toInstant(),
                "COMPLETED", new BigDecimal("50.00"), decDate.toInstant()); // Reembolso parcial de 50.00 de 200.00
        insertOrder(STORE_A, USER_ID_1, paymentId, capResId, "CONFIRMED", new BigDecimal("200.00"), decDate.toInstant());

        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 1));
        FinancialSummary fin = repository.findFinancialSummary(period, STORE_A, null);

        assertThat(fin.grossInflowAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(fin.settledRefundsAmount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(fin.netFinancialInflow()).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    @Order(11)
    @DisplayName("11. Agregaciones sin duplicaciones por joins con múltiples order_items")
    void scenario11_multiItemsOrderDoesNotDuplicatePayments() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime date = ZonedDateTime.of(2026, 12, 10, 15, 0, 0, 0, LIMA);
        UUID capRes = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 12, 10), "COMMITTED");
        UUID pay = insertPayment(capRes, new BigDecimal("100.00"), "APPROVED", date.toInstant(), "NOT_REQUIRED", null, null);
        UUID order = insertOrder(STORE_A, USER_ID_1, pay, capRes, "CONFIRMED", new BigDecimal("100.00"), date.toInstant());

        UUID var1 = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf301");
        UUID var2 = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302");
        UUID var3 = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf303");

        insertOrderItem(order, STORE_A, var1, "Torta de Chocolate Artesanal", "Torta Entera", 1, new BigDecimal("30.00"), new BigDecimal("30.00"));
        insertOrderItem(order, STORE_A, var2, "Torta de Chocolate Artesanal", "Porción Individual", 1, new BigDecimal("30.00"), new BigDecimal("30.00"));
        insertOrderItem(order, STORE_A, var3, "Cheesecake de Frutos Rojos", "Porción Individual", 1, new BigDecimal("40.00"), new BigDecimal("40.00"));

        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10));

        FinancialSummary fin = repository.findFinancialSummary(period, STORE_A, null);
        CommercialSummary comm = repository.findCommercialSummary(period, STORE_A, null);
        List<ProductSalesRank> topProducts = repository.findTopSellingProducts(period, STORE_A, null, 10);

        // El pago debe contabilizarse exactamente como 100.00 (NUNCA 300.00)
        assertThat(fin.grossInflowAmount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(fin.approvedPaymentsCount()).isEqualTo(1L);

        // El pedido es 1 solo y GMV es 100.00
        assertThat(comm.historicalConfirmedOrdersCount()).isEqualTo(1L);
        assertThat(comm.historicalConfirmedGmv()).isEqualTo(new BigDecimal("100.00"));

        // Se deben registrar los 3 productos en el ranking
        assertThat(topProducts).hasSize(3);
    }

    @Test
    @Order(12)
    @DisplayName("12. GMV histórico confirmado inmutable tras cancelación posterior")
    void scenario12_historicalGmvPreservedAfterSubsequentCancellation() throws SQLException {
        ensureUserAndStore(USER_ID_1, STORE_A, "Store A");

        ZonedDateTime confirmDate = ZonedDateTime.of(2026, 7, 10, 10, 0, 0, 0, LIMA);
        ZonedDateTime cancelDate = ZonedDateTime.of(2026, 7, 28, 15, 0, 0, 0, LIMA);

        UUID capRes = insertCapacityReservation(STORE_A, USER_ID_1, LocalDate.of(2026, 7, 10), "RELEASED");
        UUID pay = insertPayment(capRes, new BigDecimal("150.00"), "APPROVED", confirmDate.toInstant(), "PENDING", new BigDecimal("150.00"), null);
        UUID orderId = insertOrder(STORE_A, USER_ID_1, pay, capRes, "CANCELLED", new BigDecimal("150.00"), confirmDate.toInstant());
        insertStatusHistory(orderId, USER_ID_1, "CANCELLED", cancelDate.toInstant());

        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        CommercialSummary comm = repository.findCommercialSummary(period, STORE_A, null);

        // El GMV histórico confirmado preserva los 150.00
        assertThat(comm.historicalConfirmedGmv()).isEqualTo(new BigDecimal("150.00"));
        assertThat(comm.historicalConfirmedOrdersCount()).isEqualTo(1L);

        // El volumen comercial activo queda en 0.00 debido a la cancelación
        assertThat(comm.activeCommercialVolume()).isEqualTo(new BigDecimal("0.00"));
        assertThat(comm.activeOrdersCount()).isZero();

        // Las cancelaciones reflejan los 150.00 de manera independiente
        assertThat(comm.cancelledOrdersVolume()).isEqualTo(new BigDecimal("150.00"));
        assertThat(comm.cancelledOrdersCount()).isEqualTo(1L);
    }

    @Test
    @Order(13)
    @DisplayName("13. Generación y consistencia de documentos PDF desde datos reales en PostgreSQL")
    void scenario13_pdfExportEndToEndValidation() {
        PeriodFilter period = PeriodFilter.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        // 1. Reporte Customer y PDF
        CustomerAnalyticsReport customerReport = getCustomerAnalyticsUseCase.execute(USER_ID_1, period);
        byte[] customerPdf = analyticsPdfExportPort.generateCustomerReportPdf(customerReport);
        assertThat(customerPdf).isNotNull();
        assertThat(customerPdf.length).isGreaterThan(500);
        assertThat(new String(customerPdf, 0, 5)).isEqualTo("%PDF-");

        // 2. Reporte Merchant y PDF
        MerchantAnalyticsReport merchantReport = getMerchantAnalyticsUseCase.execute(STORE_A, period);
        byte[] merchantPdf = analyticsPdfExportPort.generateMerchantReportPdf(merchantReport);
        assertThat(merchantPdf).isNotNull();
        assertThat(merchantPdf.length).isGreaterThan(500);
        assertThat(new String(merchantPdf, 0, 5)).isEqualTo("%PDF-");

        // 3. Reporte Admin y PDF
        AdminPlatformAnalyticsReport adminReport = getAdminPlatformAnalyticsUseCase.execute(period, null);
        byte[] adminPdf = analyticsPdfExportPort.generateAdminReportPdf(adminReport);
        assertThat(adminPdf).isNotNull();
        assertThat(adminPdf.length).isGreaterThan(500);
        assertThat(new String(adminPdf, 0, 5)).isEqualTo("%PDF-");
    }

    // =========================================================================
    // MÉTODOS AUXILIARES DE INSERCIÓN JDBC
    // =========================================================================

    private void ensureUserAndStore(UUID userId, UUID storeId, String storeName) throws SQLException {
        createdUserIds.add(userId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            try (var ps = conn.prepareStatement("""
                    INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at)
                    VALUES (?, ?, '$2a$10$abcdefghijklmnopqrstuv', 'Test', 'User', 'ACTIVE', NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, userId);
                ps.setString(2, "user-" + userId + "@jaldi.test");
                ps.executeUpdate();
            }

            try (var ps = conn.prepareStatement("""
                    INSERT INTO user_roles (id, user_id, role_id, store_id)
                    VALUES (gen_random_uuid(), ?, 1, ?)
                    ON CONFLICT DO NOTHING;
                    """)) {
                ps.setObject(1, userId);
                ps.setObject(2, storeId);
                ps.executeUpdate();
            }
        }
    }

    private UUID insertCapacityReservation(UUID storeId, UUID userId, LocalDate serviceDate, String status) throws SQLException {
        UUID id = UUID.randomUUID();
        createdCapResIds.add(id);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (var ps = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, '12:00:00', '13:00:00', ?, NOW() + INTERVAL '1 day', NOW(), NOW());
                    """)) {
                ps.setObject(1, id);
                ps.setObject(2, storeId);
                ps.setObject(3, userId);
                ps.setObject(4, serviceDate);
                ps.setString(5, status);
                ps.executeUpdate();
            }
        }
        return id;
    }

    private UUID insertPayment(UUID capResId, BigDecimal amount, String status, java.time.Instant approvedAt,
                               String refundStatus, BigDecimal refundAmount, java.time.Instant refundedAt) throws SQLException {
        UUID id = UUID.randomUUID();
        createdPaymentIds.add(id);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (var ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, refund_amount, approved_at, refunded_at, created_at, updated_at)
                    VALUES (?, ?, ?, 'PEN', ?, ?, ?, ?, ?, NOW(), NOW());
                    """)) {
                ps.setObject(1, id);
                ps.setObject(2, capResId);
                ps.setBigDecimal(3, amount);
                ps.setString(4, status);
                ps.setString(5, refundStatus);
                ps.setBigDecimal(6, refundAmount);
                ps.setTimestamp(7, approvedAt != null ? Timestamp.from(approvedAt) : null);
                ps.setTimestamp(8, refundedAt != null ? Timestamp.from(refundedAt) : null);
                ps.executeUpdate();
            }
        }
        return id;
    }

    private UUID insertOrder(UUID storeId, UUID userId, UUID paymentId, UUID capResId, String status,
                             BigDecimal totalAmount, java.time.Instant confirmedAt) throws SQLException {
        UUID id = UUID.randomUUID();
        createdOrderIds.add(id);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (var ps = conn.prepareStatement("""
                    INSERT INTO orders (id, order_number, user_id, store_id, payment_id, capacity_reservation_id, status, delivery_mode, service_date,
                                        customer_name, customer_phone, customer_email,
                                        products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, total_amount, confirmed_at, updated_at, currency)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'PICKUP', '2026-10-15',
                            'Customer Test', '999888777', 'test@jaldi.test',
                            ?, 0, 0, 0, ?, ?, NOW(), 'PEN');
                    """)) {
                ps.setObject(1, id);
                ps.setString(2, "ANL-" + id.toString().substring(0, 8));
                ps.setObject(3, userId);
                ps.setObject(4, storeId);
                ps.setObject(5, paymentId);
                ps.setObject(6, capResId);
                ps.setString(7, status);
                ps.setBigDecimal(8, totalAmount);
                ps.setBigDecimal(9, totalAmount);
                ps.setTimestamp(10, Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }
        }
        return id;
    }

    private void insertStatusHistory(UUID orderId, UUID userId, String status, java.time.Instant changedAt) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (var ps = conn.prepareStatement("""
                    INSERT INTO order_status_history (id, order_id, changed_by_user_id, status, reason, changed_at)
                    VALUES (gen_random_uuid(), ?, ?, ?, 'Cancelación test', ?);
                    """)) {
                ps.setObject(1, orderId);
                ps.setObject(2, userId);
                ps.setString(3, status);
                ps.setTimestamp(4, Timestamp.from(changedAt));
                ps.executeUpdate();
            }
        }
    }

    private void insertRefundIntent(UUID intentId, UUID paymentId, BigDecimal amount, java.time.Instant completedAt) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (var ps = conn.prepareStatement("""
                    INSERT INTO payment_refund_intents (id, payment_id, checkout_mode, amount, currency, idempotency_key, status, completed_at, created_at, updated_at)
                    VALUES (?, ?, 'PREFERENCES', ?, 'PEN', ?, 'COMPLETED', ?, NOW(), NOW());
                    """)) {
                ps.setObject(1, intentId);
                ps.setObject(2, paymentId);
                ps.setBigDecimal(3, amount);
                ps.setString(4, "idemp-" + intentId.toString().substring(0, 8));
                ps.setTimestamp(5, Timestamp.from(completedAt));
                ps.executeUpdate();
            }
        }
    }

    private void insertExecutionAttempt(UUID intentId, int attemptNumber, String status, java.time.Instant executedAt) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (var ps = conn.prepareStatement("""
                    INSERT INTO payment_refund_execution_attempts (id, refund_intent_id, attempt_number, request_method, status, executed_at)
                    VALUES (gen_random_uuid(), ?, ?, 'POST', ?, ?);
                    """)) {
                ps.setObject(1, intentId);
                ps.setInt(2, attemptNumber);
                ps.setString(3, status);
                ps.setTimestamp(4, Timestamp.from(executedAt));
                ps.executeUpdate();
            }
        }
    }

    private UUID insertVariant(UUID storeId, String prodName, String varName, BigDecimal price) throws SQLException {
        UUID prodId = UUID.randomUUID();
        UUID varId = UUID.randomUUID();
        createdVariantIds.add(varId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            // Producto en catalogo
            try (var ps = conn.prepareStatement("""
                    INSERT INTO products (id, store_id, category_id, name, description, status, created_at, updated_at)
                    VALUES (?, ?, (SELECT id FROM categories WHERE store_id = ? LIMIT 1), ?, 'Desc', 'ACTIVE', NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, prodId);
                ps.setObject(2, storeId);
                ps.setObject(3, storeId);
                ps.setString(4, prodName);
                ps.executeUpdate();
            }

            // Variante
            try (var ps = conn.prepareStatement("""
                    INSERT INTO product_variants (id, product_id, presentation_name, price_amount, price_currency, tracks_inventory, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'PEN', false, 'ACTIVE', NOW(), NOW());
                    """)) {
                ps.setObject(1, varId);
                ps.setObject(2, prodId);
                ps.setString(3, varName);
                ps.setBigDecimal(4, price);
                ps.executeUpdate();
            }
        }
        return varId;
    }

    private void insertOrderItem(UUID orderId, UUID storeId, UUID variantId, String prodName, String varName,
                                 int qty, BigDecimal unitPrice, BigDecimal subtotal) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (var ps = conn.prepareStatement("""
                    INSERT INTO order_items (id, order_id, store_id, variant_id, product_name, variant_name, quantity, unit_price_amount, subtotal_amount)
                    VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, ?, ?, ?);
                    """)) {
                ps.setObject(1, orderId);
                ps.setObject(2, storeId);
                ps.setObject(3, variantId);
                ps.setString(4, prodName);
                ps.setString(5, varName);
                ps.setInt(6, qty);
                ps.setBigDecimal(7, unitPrice);
                ps.setBigDecimal(8, subtotal);
                ps.executeUpdate();
            }
        }
    }
}
