package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica contra PostgreSQL real la sincronización de ConfirmPurchase (BE-20):
 * lock de la reserva de capacidad, decremento atómico condicional de inventario,
 * y los UNIQUE de orders como backstop de idempotencia. Replica el protocolo SQL
 * de CommitCapacityReservationService + CommitInventoryReservationService (sin
 * levantar contexto Spring).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConfirmPurchaseConcurrencyIntegrationTest {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean dbAvailable = false;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID PRODUCT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf201");

    @BeforeAll
    static void initDatabase() throws SQLException {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        dbUrl = config.url();
        dbUser = config.username();
        dbPassword = config.password();

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            Flyway flyway = Flyway.configure()
                    .dataSource(dbUrl, dbUser, dbPassword)
                    .locations("classpath:db/migration")
                    .validateOnMigrate(config.validateOnMigrate())
                    .load();
            MigrateResult result = flyway.migrate();
            System.out.println("ConfirmPurchaseConcurrencyIntegrationTest: Flyway migration completed (" + result.migrationsExecuted + " migrations executed).");
            dbAvailable = true;
            cleanUpTestData();
        }
    }

    @AfterAll
    static void cleanUpTestData() {
        if (!dbAvailable) return;
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("""
                    DELETE FROM order_status_history
                    WHERE order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'user-%@example.com'));
                    """);
                stmt.executeUpdate("""
                    DELETE FROM order_items
                    WHERE order_id IN (SELECT id FROM orders WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'user-%@example.com'));
                    """);
                stmt.executeUpdate("""
                    DELETE FROM orders
                    WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'user-%@example.com');
                    """);
                stmt.executeUpdate("""
                    DELETE FROM payments
                    WHERE capacity_reservation_id IN (
                        SELECT id FROM capacity_reservations
                        WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'user-%@example.com')
                    );
                    """);
                stmt.executeUpdate("""
                    DELETE FROM inventory_reservations
                    WHERE capacity_reservation_id IN (
                        SELECT id FROM capacity_reservations
                        WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'user-%@example.com')
                    )
                    OR variant_id IN (SELECT id FROM product_variants WHERE presentation_name = 'Variante Confirm');
                    """);
                stmt.executeUpdate("""
                    DELETE FROM capacity_reservations
                    WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'user-%@example.com');
                    """);
                stmt.executeUpdate("""
                    DELETE FROM inventories
                    WHERE variant_id IN (SELECT id FROM product_variants WHERE presentation_name = 'Variante Confirm');
                    """);
                stmt.executeUpdate("""
                    DELETE FROM product_variants
                    WHERE presentation_name = 'Variante Confirm' OR sku LIKE 'SKU-CONFIRM-%';
                    """);
                stmt.executeUpdate("""
                    DELETE FROM users
                    WHERE email LIKE 'user-%@example.com';
                    """);
                System.out.println("ConfirmPurchaseConcurrencyIntegrationTest: Datos de prueba limpiados exitosamente.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Test database cleanup failed.", e);
        }
    }

    private Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        conn.setAutoCommit(false);
        return conn;
    }

    private static void awaitFutures(Future<?>... futures) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        for (Future<?> future : futures) {
            try {
                future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            } catch (ExecutionException e) {
                throw new AssertionError("Concurrent confirmation failed unexpectedly.", e.getCause());
            }
        }
    }

    private void createTestUser(Connection conn, UUID userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at)
                VALUES (?, ?, '$2a$10$abcdefghijklmnopqrstuv', 'Test', 'User', 'ACTIVE', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING;
                """)) {
            ps.setObject(1, userId);
            ps.setString(2, "user-" + userId + "@example.com");
            ps.executeUpdate();
        }
    }

    private void createVariant(Connection conn, UUID variantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO product_variants (id, store_id, product_id, presentation_name, sku, price_amount, price_currency, tracks_inventory, status, created_at, updated_at)
                VALUES (?, ?, ?, 'Variante Confirm', ?, 10.00, 'PEN', true, 'ACTIVE', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING;
                """)) {
            ps.setObject(1, variantId);
            ps.setObject(2, STORE_ID);
            ps.setObject(3, PRODUCT_ID);
            ps.setString(4, "SKU-CONFIRM-" + UUID.randomUUID());
            ps.executeUpdate();
        }
    }

    private void setStock(Connection conn, UUID variantId, int quantity) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at)
                VALUES (?, ?, 2, NOW())
                ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity, updated_at = NOW();
                """)) {
            ps.setObject(1, variantId);
            ps.setInt(2, quantity);
            ps.executeUpdate();
        }
    }

    private UUID insertCapacityReservation(Connection conn, UUID userId, String status,
                                            int expiresMinOffset, Integer protectionMinOffset) throws SQLException {
        UUID capResId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_DATE + INTERVAL '30 days', '10:00:00', '12:00:00', ?, NOW() + (? * INTERVAL '1 minute'), NOW() + (? * INTERVAL '1 minute'), NOW(), NOW());
                """)) {
            ps.setObject(1, capResId);
            ps.setObject(2, STORE_ID);
            ps.setObject(3, userId);
            ps.setString(4, status);
            ps.setInt(5, expiresMinOffset);
            ps.setObject(6, protectionMinOffset);
            ps.executeUpdate();
        }
        return capResId;
    }

    private void insertInventoryHold(Connection conn, UUID capResId, UUID variantId, int quantity, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW() + INTERVAL '10 minutes', NOW(), NOW());
                """)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, STORE_ID);
            ps.setObject(3, capResId);
            ps.setObject(4, variantId);
            ps.setInt(5, quantity);
            ps.setString(6, status);
            ps.executeUpdate();
        }
    }

    private void insertPayment(Connection conn, UUID capResId, String status) throws SQLException {
        UUID paymentId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, approved_at, created_at, updated_at)
                VALUES (?, ?, 20.00, 'PEN', ?, 'NOT_REQUIRED', NOW(), NOW(), NOW());
                """)) {
            ps.setObject(1, paymentId);
            ps.setObject(2, capResId);
            ps.setString(3, status);
            ps.executeUpdate();
        }
    }

    private void insertOrder(Connection conn, UUID capResId, UUID userId, UUID variantId, UUID paymentId,
                             String orderNumber, String status) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO orders (id, order_number, user_id, store_id, payment_id, capacity_reservation_id, status, delivery_mode, service_date, service_start_time, service_end_time,
                                    customer_name, customer_phone, customer_email, delivery_address, delivery_reference, currency,
                                    products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, total_amount, confirmed_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PICKUP', CURRENT_DATE + INTERVAL '30 days', '10:00:00', '12:00:00',
                        'Juan Test', '987654321', ?, NULL, NULL, 'PEN',
                        20.00, 0.00, 0.00, 3.05, 20.00, NOW(), NOW());
                """)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, orderNumber);
            ps.setObject(3, userId);
            ps.setObject(4, STORE_ID);
            ps.setObject(5, paymentId);
            ps.setObject(6, capResId);
            ps.setString(7, status);
            ps.setString(8, "user-" + userId + "@example.com");
            ps.executeUpdate();
        }
    }

    private String newOrderNumber() {
        return "JAL-" + UUID.randomUUID();
    }

    /**
     * Replica el protocolo de CommitCapacityReservationService + CommitInventoryReservationService:
     * lock PESSIMISTIC_WRITE de la reserva, validar estado, locks de inventario en orden
     * canónico ASC, decremento atómico condicional con guardantía de otros holds activos,
     * marca COMMITTED e inserta el pedido + detalle. Devuelve "COMMITTED" o "SKIP_COMMITTED".
     */
    private String executeConfirm(Connection conn, UUID capResId, UUID storeId, UUID userId,
                                  UUID variantId, int quantity, UUID paymentId, String orderNumber) throws SQLException {
        try {
            String reservationStatus;
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT status, payment_protection_expires_at FROM capacity_reservations WHERE id = ? FOR UPDATE;
                    """)) {
                ps.setObject(1, capResId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("RESERVATION_NOT_FOUND");
                    }
                    reservationStatus = rs.getString("status");
                    Timestamp protectionExpiresAt = rs.getTimestamp("payment_protection_expires_at");
                    if ("PAYMENT_PROTECTED".equals(reservationStatus)
                            && (protectionExpiresAt == null || protectionExpiresAt.toInstant().isBefore(java.time.Instant.now()))) {
                        throw new IllegalStateException("RESERVATION_PROTECTION_EXPIRED");
                    }
                    if ("COMMITTED".equals(reservationStatus)) {
                        return "SKIP_COMMITTED";
                    }
                    if (!"PAYMENT_PROTECTED".equals(reservationStatus)) {
                        throw new IllegalStateException("RESERVATION_NOT_PROTECTED:" + reservationStatus);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM inventories WHERE variant_id = ? FOR UPDATE;")) {
                ps.setObject(1, variantId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("INVENTORY_NOT_CONFIGURED");
                    }
                }
            }

            int otherActive;
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COALESCE(SUM(ir.quantity), 0)
                    FROM inventory_reservations ir
                    JOIN capacity_reservations cr ON ir.capacity_reservation_id = cr.id
                    WHERE ir.store_id = ?
                      AND ir.variant_id = ?
                      AND ir.status = 'ACTIVE'
                      AND ir.capacity_reservation_id <> ?
                      AND (
                           (cr.status = 'ACTIVE' AND cr.expires_at > NOW())
                        OR (cr.status = 'PAYMENT_PROTECTED' AND cr.payment_protection_expires_at IS NOT NULL AND cr.payment_protection_expires_at > NOW())
                      );
                    """)) {
                ps.setObject(1, storeId);
                ps.setObject(2, variantId);
                ps.setObject(3, capResId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    otherActive = rs.getInt(1);
                }
            }

            int affected;
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE inventories
                    SET quantity = quantity - ?, updated_at = NOW()
                    WHERE variant_id = ? AND quantity - ? - ? >= 0;
                    """)) {
                ps.setInt(1, quantity);
                ps.setObject(2, variantId);
                ps.setInt(3, quantity);
                ps.setInt(4, otherActive);
                affected = ps.executeUpdate();
            }
            if (affected == 0) {
                throw new IllegalStateException("INSUFFICIENT_STOCK");
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE inventory_reservations SET status = 'COMMITTED', updated_at = NOW()
                    WHERE capacity_reservation_id = ? AND variant_id = ?;
                    """)) {
                ps.setObject(1, capResId);
                ps.setObject(2, variantId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE capacity_reservations SET status = 'COMMITTED', updated_at = NOW() WHERE id = ?;
                    """)) {
                ps.setObject(1, capResId);
                ps.executeUpdate();
            }

            insertOrder(conn, capResId, userId, variantId, paymentId, orderNumber, "CONFIRMED");
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO order_items (id, order_id, store_id, variant_id, product_name, variant_name, quantity, unit_price_amount, subtotal_amount)
                    VALUES (?, (SELECT id FROM orders WHERE capacity_reservation_id = ?), ?, ?, 'Pan Test', 'Bolsa x10', ?, 10.00, 20.00);
                    """)) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, capResId);
                ps.setObject(3, storeId);
                ps.setObject(4, variantId);
                ps.setInt(5, quantity);
                ps.executeUpdate();
            }

            conn.commit();
            return "COMMITTED";
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }

    private int countOrders(Connection conn, UUID capResId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders WHERE capacity_reservation_id = ?;")) {
            ps.setObject(1, capResId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int getStock(Connection conn, UUID variantId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM inventories WHERE variant_id = ?;")) {
            ps.setObject(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("quantity");
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("TEST 1: 2 confirmaciones concurrentes de la misma reserva -> 1 pedido y un único decremento")
    void test1_concurrentConfirmSameReservationIsIdempotent() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID userA = UUID.randomUUID();

        UUID capResId;
        UUID paymentId;
        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, userA);
            createVariant(setupConn, variantId);
            setStock(setupConn, variantId, 5);
            capResId = insertCapacityReservation(setupConn, userA, "PAYMENT_PROTECTED", 10, 10);
            insertInventoryHold(setupConn, capResId, variantId, 2, "ACTIVE");
            insertPayment(setupConn, capResId, "APPROVED");
            setupConn.commit();
        }
        paymentId = findPaymentId(capResId);

        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger committedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        Runnable confirmTask = () -> {
            try (Connection conn = getConnection()) {
                startLatch.await();
                String outcome = executeConfirm(conn, capResId, STORE_ID, userA, variantId, 2, paymentId,
                        newOrderNumber());
                if ("COMMITTED".equals(outcome)) committedCount.incrementAndGet();
                else if ("SKIP_COMMITTED".equals(outcome)) skippedCount.incrementAndGet();
            } catch (Exception e) {
                throw new AssertionError("Confirm concurrente falló inesperadamente.", e);
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(confirmTask);
            Future<?> second = executor.submit(confirmTask);
            startLatch.countDown();
            awaitFutures(first, second);
        } finally {
            startLatch.countDown();
            executor.shutdownNow();
        }

        assertEquals(1, committedCount.get(), "Exactamente 1 confirmación debe hacer el trabajo");
        assertEquals(1, skippedCount.get(), "La otra confirmación debe detectar COMMITTED y no hacer nada");

        try (Connection conn = getConnection()) {
            assertEquals(1, countOrders(conn, capResId), "Solo debe existir 1 pedido para la reserva");
            assertEquals(3, getStock(conn, variantId), "Stock debe decrementarse exactamente una vez (5 - 2 = 3)");
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM capacity_reservations WHERE id = ?;")) {
                ps.setObject(1, capResId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals("COMMITTED", rs.getString(1));
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(2)
    @DisplayName("TEST 2: el hold propio se excluye del conteo de 'otros activos' al descontar")
    void test2_ownHoldExcludedFromOtherActiveGuard() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        UUID capResId;
        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, user);
            createVariant(setupConn, variantId);
            setStock(setupConn, variantId, 5);
            capResId = insertCapacityReservation(setupConn, user, "PAYMENT_PROTECTED", 10, 10);
            insertInventoryHold(setupConn, capResId, variantId, 3, "ACTIVE");
            insertPayment(setupConn, capResId, "APPROVED");
            setupConn.commit();
        }
        UUID paymentId = findPaymentId(capResId);

        try (Connection conn = getConnection()) {
            String outcome = executeConfirm(conn, capResId, STORE_ID, user, variantId, 3, paymentId, newOrderNumber());
            assertEquals("COMMITTED", outcome);
        }

        try (Connection conn = getConnection()) {
            assertEquals(2, getStock(conn, variantId), "5 - 3 = 2; el hold propio no debe bloquear su propio decremento");
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM inventory_reservations WHERE capacity_reservation_id = ? AND variant_id = ?;")) {
                ps.setObject(1, capResId);
                ps.setObject(2, variantId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals("COMMITTED", rs.getString(1));
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(3)
    @DisplayName("TEST 3: otro hold ACTIVE vigente bloquea el oversell -> rollback total sin pedido")
    void test3_otherActiveHoldPreventsOversell() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();

        UUID capResId;
        UUID otherCapResId;
        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, user);
            createTestUser(setupConn, otherUser);
            createVariant(setupConn, variantId);
            setStock(setupConn, variantId, 3);

            capResId = insertCapacityReservation(setupConn, user, "PAYMENT_PROTECTED", 10, 10);
            insertInventoryHold(setupConn, capResId, variantId, 3, "ACTIVE");
            insertPayment(setupConn, capResId, "APPROVED");

            otherCapResId = insertCapacityReservation(setupConn, otherUser, "ACTIVE", 10, null);
            insertInventoryHold(setupConn, otherCapResId, variantId, 3, "ACTIVE");
            setupConn.commit();
        }
        UUID paymentId = findPaymentId(capResId);

        try (Connection conn = getConnection()) {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    executeConfirm(conn, capResId, STORE_ID, user, variantId, 3, paymentId, newOrderNumber()));
            assertTrue(ex.getMessage().contains("INSUFFICIENT_STOCK"));
        }

        try (Connection conn = getConnection()) {
            assertEquals(3, getStock(conn, variantId), "Ningún stock debe descontarse (rollback total)");
            assertEquals(0, countOrders(conn, capResId), "No debe crearse pedido");
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM capacity_reservations WHERE id = ?;")) {
                ps.setObject(1, capResId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals("PAYMENT_PROTECTED", rs.getString(1), "La reserva debe seguir protegida sin comprometer");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) FROM inventory_reservations
                    WHERE variant_id = ? AND status = 'ACTIVE';
                    """)) {
                ps.setObject(1, variantId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(2, rs.getInt(1), "Ambos holds deben seguir ACTIVE");
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(4)
    @DisplayName("TEST 4: protección de pago vencida -> no se compromete ni decrementa")
    void test4_expiredPaymentProtectionNeverCommits() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        UUID capResId;
        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, user);
            createVariant(setupConn, variantId);
            setStock(setupConn, variantId, 5);
            capResId = insertCapacityReservation(setupConn, user, "PAYMENT_PROTECTED", 10, -1);
            insertInventoryHold(setupConn, capResId, variantId, 2, "ACTIVE");
            insertPayment(setupConn, capResId, "APPROVED");
            setupConn.commit();
        }
        UUID paymentId = findPaymentId(capResId);

        try (Connection conn = getConnection()) {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    executeConfirm(conn, capResId, STORE_ID, user, variantId, 2, paymentId, newOrderNumber()));
            assertTrue(ex.getMessage().contains("RESERVATION_PROTECTION_EXPIRED"));
        }

        try (Connection conn = getConnection()) {
            assertEquals(5, getStock(conn, variantId), "El stock no debe descontarse");
            assertEquals(0, countOrders(conn, capResId), "No debe crearse pedido con protección vencida");
            conn.rollback();
        }
    }

    @Test
    @Order(5)
    @DisplayName("TEST 5: UNIQUE de orders (payment_id, capacity_reservation_id) es backstop de idempotencia")
    void test5_orderUniqueBackstop() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        UUID capResId;
        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, user);
            createVariant(setupConn, variantId);
            setStock(setupConn, variantId, 5);
            capResId = insertCapacityReservation(setupConn, user, "PAYMENT_PROTECTED", 10, 10);
            insertInventoryHold(setupConn, capResId, variantId, 2, "ACTIVE");
            insertPayment(setupConn, capResId, "APPROVED");
            setupConn.commit();
        }
        UUID paymentId = findPaymentId(capResId);

        try (Connection conn = getConnection()) {
            String outcome = executeConfirm(conn, capResId, STORE_ID, user, variantId, 2, paymentId, newOrderNumber());
            assertEquals("COMMITTED", outcome);
        }

        // Segundo INSERT de pedido con el mismo capacity_reservation_id y payment_id debe fallar
        try (Connection conn = getConnection()) {
            Savepoint sp = conn.setSavepoint();
            try {
                assertThrows(SQLException.class, () ->
                        insertOrder(conn, capResId, user, variantId, paymentId, newOrderNumber(), "CONFIRMED"),
                        "El UNIQUE de capacity_reservation_id no permitió insertar un segundo pedido");
            } finally {
                conn.rollback(sp);
            }
            assertEquals(1, countOrders(conn, capResId));
            conn.rollback();
        }
    }

    private UUID findPaymentId(UUID capResId) throws SQLException {
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM payments WHERE capacity_reservation_id = ?;")) {
                ps.setObject(1, capResId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getObject("id", UUID.class);
                }
            }
        }
    }

}
