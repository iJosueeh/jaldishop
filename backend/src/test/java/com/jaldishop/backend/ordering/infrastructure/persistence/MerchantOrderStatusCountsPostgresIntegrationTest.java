package com.jaldishop.backend.ordering.infrastructure.persistence;

import com.jaldishop.backend.ordering.application.result.MerchantOrderStatusCountsResponse;
import com.jaldishop.backend.ordering.application.service.ChangeOrderStatusService;
import com.jaldishop.backend.ordering.application.service.GetMerchantOrderStatusCountsService;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.OrderStatusCounts;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración de contadores de pedidos con PostgreSQL real y schema Flyway.
 *
 * <p>Verifica:</p>
 * <ul>
 *   <li>1. Conteo histórico de todos los estados.</li>
 *   <li>2. Conteo filtrado por {@code service_date}.</li>
 *   <li>3. Aislamiento estricto entre tiendas (Tienda A vs Tienda B).</li>
 *   <li>4. Inclusión de pedidos CANCELLED en {@code all} (RN-COUNT-02).</li>
 *   <li>5. Exclusión de estados no activos en pedidos activos del Sidebar (RN-COUNT-03).</li>
 *   <li>6. Tienda sin pedidos (retorna ceros).</li>
 *   <li>7. Fecha sin pedidos (retorna ceros).</li>
 *   <li>8. Recuento actualizado tras transición de estado confirmada.</li>
 *   <li>9. Consistencia estricta de la suma de estados con {@code all}.</li>
 * </ul>
 */
@SpringBootTest
@Import(OrderPersistenceIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MerchantOrderStatusCountsPostgresIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    private static UUID storeAId;
    private static UUID storeBId;
    private static UUID emptyStoreId;
    private static UUID customerUserId;

    private static final List<UUID> createdUserIds = new ArrayList<>();
    private static final List<UUID> createdStoreIds = new ArrayList<>();
    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static final LocalDate DATE_TODAY = LocalDate.now();
    private static final LocalDate DATE_TOMORROW = DATE_TODAY.plusDays(1);
    private static final LocalDate DATE_EMPTY = DATE_TODAY.minusDays(30);

    private static UUID mutableOrderId;

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
    }

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private GetMerchantOrderStatusCountsService countsService;

    @Autowired
    private ChangeOrderStatusService changeOrderStatusService;

    @BeforeAll
    static void seedDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            customerUserId = createTestUser(conn, "customer.counts@" + UUID.randomUUID() + ".com");
            UUID ownerA = createTestUser(conn, "owner.counts.a@" + UUID.randomUUID() + ".com");
            UUID ownerB = createTestUser(conn, "owner.counts.b@" + UUID.randomUUID() + ".com");
            UUID ownerEmpty = createTestUser(conn, "owner.counts.empty@" + UUID.randomUUID() + ".com");

            storeAId = createTestStore(conn, ownerA, "store-counts-a-" + UUID.randomUUID());
            storeBId = createTestStore(conn, ownerB, "store-counts-b-" + UUID.randomUUID());
            emptyStoreId = createTestStore(conn, ownerEmpty, "store-counts-empty-" + UUID.randomUUID());
        }

        Instant baseTime = Instant.now().minus(4, ChronoUnit.HOURS);

        // =========================================================================
        // Sembrar Tienda A:
        // Fecha DATE_TODAY:
        //   - 2 CONFIRMED (uno de ellos guardado en mutableOrderId)
        //   - 1 IN_PREPARATION
        //   - 1 READY
        //   - 1 OUT_FOR_DELIVERY
        //   - 2 COMPLETED
        //   - 1 CANCELLED
        //   Subtotal DATE_TODAY = 8 pedidos
        //
        // Fecha DATE_TOMORROW:
        //   - 3 CONFIRMED
        //   - 1 COMPLETED
        //   Subtotal DATE_TOMORROW = 4 pedidos
        //
        // Total Histórico Tienda A = 12 pedidos
        // =========================================================================

        // DATE_TODAY pedidos
        mutableOrderId = UUID.randomUUID();
        seedOrder(mutableOrderId, "JAL-CNT-A-001", customerUserId, storeAId, OrderStatus.CONFIRMED, DATE_TODAY, baseTime);
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-002", customerUserId, storeAId, OrderStatus.CONFIRMED, DATE_TODAY, baseTime.plus(5, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-003", customerUserId, storeAId, OrderStatus.IN_PREPARATION, DATE_TODAY, baseTime.plus(10, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-004", customerUserId, storeAId, OrderStatus.READY, DATE_TODAY, baseTime.plus(15, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-005", customerUserId, storeAId, OrderStatus.OUT_FOR_DELIVERY, DATE_TODAY, baseTime.plus(20, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-006", customerUserId, storeAId, OrderStatus.COMPLETED, DATE_TODAY, baseTime.plus(25, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-007", customerUserId, storeAId, OrderStatus.COMPLETED, DATE_TODAY, baseTime.plus(30, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-008", customerUserId, storeAId, OrderStatus.CANCELLED, DATE_TODAY, baseTime.plus(35, ChronoUnit.MINUTES));

        // DATE_TOMORROW pedidos
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-009", customerUserId, storeAId, OrderStatus.CONFIRMED, DATE_TOMORROW, baseTime.plus(40, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-010", customerUserId, storeAId, OrderStatus.CONFIRMED, DATE_TOMORROW, baseTime.plus(45, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-011", customerUserId, storeAId, OrderStatus.CONFIRMED, DATE_TOMORROW, baseTime.plus(50, ChronoUnit.MINUTES));
        seedOrder(UUID.randomUUID(), "JAL-CNT-A-012", customerUserId, storeAId, OrderStatus.COMPLETED, DATE_TOMORROW, baseTime.plus(55, ChronoUnit.MINUTES));

        // =========================================================================
        // Sembrar Tienda B:
        // Fecha DATE_TODAY:
        //   - 5 CONFIRMED
        // Total Histórico Tienda B = 5 pedidos
        // =========================================================================
        for (int i = 0; i < 5; i++) {
            seedOrder(UUID.randomUUID(), "JAL-CNT-B-00" + (i + 1), customerUserId, storeBId, OrderStatus.CONFIRMED, DATE_TODAY, baseTime.plus(i * 2, ChronoUnit.MINUTES));
        }
    }

    @AfterAll
    static void cleanUpTestData() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            for (UUID orderId : createdOrderIds) {
                try (var ps = conn.prepareStatement("DELETE FROM order_status_history WHERE order_id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
            }

            for (UUID capResId : createdCapResIds) {
                try (var ps = conn.prepareStatement("DELETE FROM orders WHERE capacity_reservation_id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
            }

            for (UUID paymentId : createdPaymentIds) {
                try (var ps = conn.prepareStatement("DELETE FROM payments WHERE id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
            }

            for (UUID capResId : createdCapResIds) {
                try (var ps = conn.prepareStatement("DELETE FROM capacity_reservations WHERE id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
            }

            for (UUID storeId : createdStoreIds) {
                try (var ps = conn.prepareStatement("DELETE FROM stores WHERE id = ?")) {
                    ps.setObject(1, storeId);
                    ps.executeUpdate();
                }
            }

            for (UUID userId : createdUserIds) {
                try (var ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setObject(1, userId);
                    ps.executeUpdate();
                }
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("1. Conteo histórico de todos los estados para Tienda A")
    void shouldReturnCompleteHistoricalCountsForStoreA() {
        MerchantOrderStatusCountsResponse counts = countsService.execute(storeAId, null);

        assertThat(counts.confirmed()).isEqualTo(5);       // 2 de hoy + 3 de mañana
        assertThat(counts.inPreparation()).isEqualTo(1);   // 1 de hoy
        assertThat(counts.ready()).isEqualTo(1);           // 1 de hoy
        assertThat(counts.outForDelivery()).isEqualTo(1);   // 1 de hoy
        assertThat(counts.completed()).isEqualTo(3);       // 2 de hoy + 1 de mañana
        assertThat(counts.cancelled()).isEqualTo(1);       // 1 de hoy

        // Invariante RN-COUNT-02
        long expectedAll = 5 + 1 + 1 + 1 + 3 + 1; // 12
        assertThat(counts.all()).isEqualTo(expectedAll).isEqualTo(12);
    }

    @Test
    @Order(2)
    @DisplayName("2. Conteo filtrado por service_date (DATE_TODAY) para Tienda A")
    void shouldReturnDailyCountsForToday() {
        MerchantOrderStatusCountsResponse counts = countsService.execute(storeAId, DATE_TODAY);

        assertThat(counts.confirmed()).isEqualTo(2);
        assertThat(counts.inPreparation()).isEqualTo(1);
        assertThat(counts.ready()).isEqualTo(1);
        assertThat(counts.outForDelivery()).isEqualTo(1);
        assertThat(counts.completed()).isEqualTo(2);
        assertThat(counts.cancelled()).isEqualTo(1);

        // Invariante RN-COUNT-02
        long expectedAll = 2 + 1 + 1 + 1 + 2 + 1; // 8
        assertThat(counts.all()).isEqualTo(expectedAll).isEqualTo(8);
    }

    @Test
    @Order(3)
    @DisplayName("3. Aislamiento multitienda: Tienda B no incluye pedidos de Tienda A")
    void shouldGuaranteeMultitenantIsolationBetweenStores() {
        MerchantOrderStatusCountsResponse countsB = countsService.execute(storeBId, null);

        assertThat(countsB.confirmed()).isEqualTo(5);
        assertThat(countsB.inPreparation()).isZero();
        assertThat(countsB.ready()).isZero();
        assertThat(countsB.outForDelivery()).isZero();
        assertThat(countsB.completed()).isZero();
        assertThat(countsB.cancelled()).isZero();
        assertThat(countsB.all()).isEqualTo(5);
    }

    @Test
    @Order(4)
    @DisplayName("4. RN-COUNT-02: Pedidos CANCELLED se incluyen en 'all'")
    void shouldIncludeCancelledOrdersInAllTotal() {
        MerchantOrderStatusCountsResponse counts = countsService.execute(storeAId, DATE_TODAY);

        assertThat(counts.cancelled()).isGreaterThan(0);
        long sumOfOthers = counts.confirmed() + counts.inPreparation() + counts.ready()
                + counts.outForDelivery() + counts.completed();
        assertThat(counts.all()).isEqualTo(sumOfOthers + counts.cancelled());
    }

    @Test
    @Order(5)
    @DisplayName("5. RN-COUNT-03: activeOrdersCount excluye OUT_FOR_DELIVERY, COMPLETED y CANCELLED")
    void shouldExcludeInactiveStatusesFromActiveOrdersCount() {
        OrderStatusCounts domainCounts = orderRepository.countByStoreIdAndServiceDate(storeAId, DATE_TODAY);

        // Hoy en Tienda A: 2 confirmed, 1 inPreparation, 1 ready, 1 outForDelivery, 2 completed, 1 cancelled
        long activeCount = domainCounts.activeOrdersCount();
        assertThat(activeCount).isEqualTo(2 + 1 + 1); // 4
    }

    @Test
    @Order(6)
    @DisplayName("6. Tienda sin pedidos retorna todos los contadores en 0")
    void shouldReturnAllZerosForEmptyStore() {
        MerchantOrderStatusCountsResponse counts = countsService.execute(emptyStoreId, null);

        assertThat(counts.all()).isZero();
        assertThat(counts.confirmed()).isZero();
        assertThat(counts.inPreparation()).isZero();
        assertThat(counts.ready()).isZero();
        assertThat(counts.outForDelivery()).isZero();
        assertThat(counts.completed()).isZero();
        assertThat(counts.cancelled()).isZero();
    }

    @Test
    @Order(7)
    @DisplayName("7. Fecha sin pedidos retorna todos los contadores en 0")
    void shouldReturnAllZerosForDateWithNoOrders() {
        MerchantOrderStatusCountsResponse counts = countsService.execute(storeAId, DATE_EMPTY);

        assertThat(counts.all()).isZero();
        assertThat(counts.confirmed()).isZero();
        assertThat(counts.inPreparation()).isZero();
        assertThat(counts.ready()).isZero();
        assertThat(counts.outForDelivery()).isZero();
        assertThat(counts.completed()).isZero();
        assertThat(counts.cancelled()).isZero();
    }

    @Test
    @Order(8)
    @DisplayName("8. Recuento actualizado tras una transición de estado confirmada (CONFIRMED -> IN_PREPARATION)")
    void shouldReflectUpdatedCountsAfterStateTransition() {
        // Antes de la transición:
        MerchantOrderStatusCountsResponse before = countsService.execute(storeAId, DATE_TODAY);
        long confirmedBefore = before.confirmed();
        long inPrepBefore = before.inPreparation();
        long allBefore = before.all();

        // Ejecutar transición válida con aislamiento de tienda verificado
        changeOrderStatusService.execute(mutableOrderId, OrderStatus.IN_PREPARATION, storeAId);

        // Después de la transición:
        MerchantOrderStatusCountsResponse after = countsService.execute(storeAId, DATE_TODAY);

        assertThat(after.confirmed()).isEqualTo(confirmedBefore - 1);
        assertThat(after.inPreparation()).isEqualTo(inPrepBefore + 1);
        assertThat(after.all()).isEqualTo(allBefore); // Total permanece constante
    }

    @Test
    @Order(9)
    @DisplayName("9. Consistencia estricta: all = confirmed + inPrep + ready + outForDelivery + completed + cancelled")
    void shouldVerifyAllEqualsSumOfAllStatusesStrictly() {
        MerchantOrderStatusCountsResponse counts = countsService.execute(storeAId, null);

        long manualSum = counts.confirmed()
                + counts.inPreparation()
                + counts.ready()
                + counts.outForDelivery()
                + counts.completed()
                + counts.cancelled();

        assertThat(counts.all()).isEqualTo(manualSum);
    }

    // =========================================================================
    // Métodos auxiliares de semilla y creación
    // =========================================================================

    private static UUID createTestUser(Connection conn, String email) throws SQLException {
        UUID userId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'hash123', 'Test', 'User', 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, userId);
            ps.setString(2, email);
            ps.executeUpdate();
        }
        createdUserIds.add(userId);
        return userId;
    }

    private static UUID createTestStore(Connection conn, UUID ownerUserId, String slug) throws SQLException {
        UUID storeId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO stores (id, owner_user_id, name, slug, pickup_enabled, delivery_enabled, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'Tienda Test Counts', ?, true, false, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, storeId);
            ps.setObject(2, ownerUserId);
            ps.setString(3, slug);
            ps.executeUpdate();
        }
        createdStoreIds.add(storeId);
        return storeId;
    }

    private static void seedOrder(UUID orderId, String orderNumber, UUID userId, UUID storeId,
                                  OrderStatus status, LocalDate serviceDate, Instant confirmedAt) throws SQLException {
        UUID capResId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        createdOrderIds.add(orderId);
        createdCapResIds.add(capResId);
        createdPaymentIds.add(paymentId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Capacity reservation
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, '10:00:00', '12:00:00', 'COMMITTED', NOW() + INTERVAL '30 minutes', NULL, ?, ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, capResId);
                ps.setObject(2, storeId);
                ps.setObject(3, userId);
                ps.setObject(4, serviceDate);
                ps.setObject(5, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(6, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }

            // 2. Payment
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, approved_at, created_at, updated_at)
                    VALUES (?, ?, 50.00, 'PEN', 'APPROVED', 'NOT_REQUIRED', ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, paymentId);
                ps.setObject(2, capResId);
                ps.setObject(3, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(4, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(5, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }

            // 3. Order
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO orders (id, order_number, user_id, store_id, payment_id, capacity_reservation_id, status, delivery_mode, service_date, service_start_time, service_end_time,
                                        customer_name, customer_phone, customer_email, delivery_address, delivery_reference, currency,
                                        products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, total_amount, confirmed_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'PICKUP', ?, '10:00:00', '12:00:00',
                            'Cliente Test', '987654321', 'test@jaldi.pe', NULL, NULL, 'PEN',
                            50.00, 0.00, 0.00, 7.63, 50.00, ?, ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, orderId);
                ps.setString(2, orderNumber);
                ps.setObject(3, userId);
                ps.setObject(4, storeId);
                ps.setObject(5, paymentId);
                ps.setObject(6, capResId);
                ps.setString(7, status.name());
                ps.setObject(8, serviceDate);
                ps.setObject(9, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(10, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }

            // 4. Status history
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO order_status_history (id, order_id, changed_by_user_id, status, reason, changed_at)
                    VALUES (?, ?, NULL, ?, 'Semilla de prueba', ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, orderId);
                ps.setString(3, status.name());
                ps.setObject(4, java.sql.Timestamp.from(confirmedAt));
                ps.executeUpdate();
            }
        }
    }
}
