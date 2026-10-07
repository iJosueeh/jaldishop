package com.jaldishop.backend.checkout.application;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CheckoutHoldConcurrencyIntegrationTest {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean dbAvailable = false;

    // Fixed store, product and capacity configuration seeded in V4
    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID PRODUCT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf201");
    private static final UUID CAPACITY_CONFIG_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9c1");

    @BeforeAll
    static void initDatabase() {
        String envDir = new File("backend/.env").exists() ? "./backend" : "./";
        Dotenv dotenv = Dotenv.configure()
                .directory(envDir)
                .ignoreIfMissing()
                .load();

        dbUrl = dotenv.get("DB_URL", System.getenv("DB_URL"));
        dbUser = dotenv.get("DB_USERNAME", System.getenv("DB_USERNAME"));
        dbPassword = dotenv.get("DB_PASSWORD", System.getenv("DB_PASSWORD"));

        if (dbUrl != null && !dbUrl.isBlank() && dbUser != null) {
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
                dbAvailable = true;
            } catch (Exception e) {
                System.err.println("Database connection failed: " + e.getMessage());
                dbAvailable = false;
            }
        }
    }

    private Connection getConnection() throws SQLException {
        Assumptions.assumeTrue(dbAvailable, "Base de datos PostgreSQL no disponible para integration tests.");
        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        conn.setAutoCommit(false);
        return conn;
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

    private void createVariant(Connection conn, UUID variantId, boolean tracksInventory) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO product_variants (id, store_id, product_id, presentation_name, sku, price_amount, price_currency, tracks_inventory, status, created_at, updated_at)
                VALUES (?, ?, ?, 'Variante Test', ?, 15.00, 'PEN', ?, 'ACTIVE', NOW(), NOW())
                ON CONFLICT (id) DO UPDATE SET tracks_inventory = EXCLUDED.tracks_inventory;
                """)) {
            ps.setObject(1, variantId);
            ps.setObject(2, STORE_ID);
            ps.setObject(3, PRODUCT_ID);
            ps.setString(4, "SKU-" + UUID.randomUUID());
            ps.setBoolean(5, tracksInventory);
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

    private record VariantItem(UUID variantId, int quantity, boolean tracksInventory) {}

    /**
     * Ejecuta el protocolo completo de checkout hold dentro de una conexión transaccional JDBC.
     */
    private UUID executeCheckoutHold(
            Connection conn,
            UUID storeId,
            UUID userId,
            UUID configId,
            LocalDate serviceDate,
            LocalTime startTime,
            LocalTime endTime,
            List<VariantItem> items
    ) throws SQLException {
        try {
            // 1. Lock capacity anchor (PESSIMISTIC_WRITE)
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT id, max_capacity FROM capacity_configurations WHERE id = ? FOR UPDATE;
                    """)) {
                ps.setObject(1, configId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("CAPACITY_UNAVAILABLE: configuración no encontrada");
                    }
                    int maxCapacity = rs.getInt("max_capacity");

                    // 2. existsReservedByUserAndWindow
                    try (PreparedStatement psExists = conn.prepareStatement("""
                            SELECT COUNT(*) FROM capacity_reservations
                            WHERE store_id = ? AND user_id = ? AND service_date = ? AND start_time = ? AND end_time = ?
                              AND (
                                   (status = 'ACTIVE' AND expires_at > NOW())
                                OR (status = 'PAYMENT_PROTECTED' AND payment_protection_expires_at IS NOT NULL AND payment_protection_expires_at > NOW())
                              );
                            """)) {
                        psExists.setObject(1, storeId);
                        psExists.setObject(2, userId);
                        psExists.setObject(3, serviceDate);
                        psExists.setObject(4, startTime);
                        psExists.setObject(5, endTime);
                        try (ResultSet rsExists = psExists.executeQuery()) {
                            rsExists.next();
                            if (rsExists.getInt(1) > 0) {
                                throw new IllegalStateException("ALREADY_RESERVED: ya existe una reserva activa para este usuario");
                            }
                        }
                    }

                    // 3. Verificar cupos disponibles (reserved + committed)
                    int reservedCount;
                    try (PreparedStatement psRes = conn.prepareStatement("""
                            SELECT COUNT(*) FROM capacity_reservations
                            WHERE store_id = ? AND service_date = ? AND start_time = ? AND end_time = ?
                              AND (
                                   (status = 'ACTIVE' AND expires_at > NOW())
                                OR (status = 'PAYMENT_PROTECTED' AND payment_protection_expires_at IS NOT NULL AND payment_protection_expires_at > NOW())
                              );
                            """)) {
                        psRes.setObject(1, storeId);
                        psRes.setObject(2, serviceDate);
                        psRes.setObject(3, startTime);
                        psRes.setObject(4, endTime);
                        try (ResultSet rsRes = psRes.executeQuery()) {
                            rsRes.next();
                            reservedCount = rsRes.getInt(1);
                        }
                    }

                    int committedCount;
                    try (PreparedStatement psCom = conn.prepareStatement("""
                            SELECT COUNT(*) FROM capacity_reservations
                            WHERE store_id = ? AND service_date = ? AND start_time = ? AND end_time = ? AND status = 'COMMITTED';
                            """)) {
                        psCom.setObject(1, storeId);
                        psCom.setObject(2, serviceDate);
                        psCom.setObject(3, startTime);
                        psCom.setObject(4, endTime);
                        try (ResultSet rsCom = psCom.executeQuery()) {
                            rsCom.next();
                            committedCount = rsCom.getInt(1);
                        }
                    }

                    if (maxCapacity - reservedCount - committedCount <= 0) {
                        throw new IllegalStateException("CAPACITY_EXHAUSTED: sin cupos de capacidad");
                    }
                }
            }

            // 4. Crear capacity_reservation (ACTIVE)
            UUID capacityReservationId = UUID.randomUUID();
            try (PreparedStatement psCap = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', NOW() + INTERVAL '10 minutes', NOW(), NOW());
                    """)) {
                psCap.setObject(1, capacityReservationId);
                psCap.setObject(2, storeId);
                psCap.setObject(3, userId);
                psCap.setObject(4, serviceDate);
                psCap.setObject(5, startTime);
                psCap.setObject(6, endTime);
                psCap.executeUpdate();
            }

            // 5. Agrupar ítems donde tracksInventory = true por variant_id
            Map<UUID, Integer> requestedQuantities = items.stream()
                    .filter(VariantItem::tracksInventory)
                    .collect(Collectors.groupingBy(VariantItem::variantId, Collectors.summingInt(VariantItem::quantity)));

            if (!requestedQuantities.isEmpty()) {
                // 6. Orden canónico obligatorio variant_id ASC
                List<UUID> sortedVariantIds = requestedQuantities.keySet().stream().sorted().toList();

                // 7. Lock pesimista en inventories (SELECT ... ORDER BY variant_id ASC FOR UPDATE)
                Map<UUID, Integer> inventoryStocks = new HashMap<>();
                for (UUID vId : sortedVariantIds) {
                    try (PreparedStatement psLock = conn.prepareStatement("""
                            SELECT variant_id, quantity FROM inventories WHERE variant_id = ? FOR UPDATE;
                            """)) {
                        psLock.setObject(1, vId);
                        try (ResultSet rsLock = psLock.executeQuery()) {
                            if (rsLock.next()) {
                                inventoryStocks.put(vId, rsLock.getInt("quantity"));
                            }
                        }
                    }
                }

                // 8. Validar existencia de todas las variantes tracked (INVENTORY_NOT_CONFIGURED)
                for (UUID vId : sortedVariantIds) {
                    if (!inventoryStocks.containsKey(vId)) {
                        throw new IllegalStateException("INVENTORY_NOT_CONFIGURED: variante " + vId);
                    }
                }

                // 9. VALIDATE ALL: validar disponibilidad de TODAS antes de insertar
                for (UUID vId : sortedVariantIds) {
                    int requested = requestedQuantities.get(vId);
                    int stock = inventoryStocks.get(vId);

                    int reservedQuantity;
                    try (PreparedStatement psSum = conn.prepareStatement("""
                            SELECT COALESCE(SUM(ir.quantity), 0)
                            FROM inventory_reservations ir
                            JOIN capacity_reservations cr ON ir.capacity_reservation_id = cr.id
                            WHERE ir.capacity_reservation_id = cr.id
                              AND ir.store_id = cr.store_id
                              AND ir.store_id = ?
                              AND ir.variant_id = ?
                              AND ir.status = 'ACTIVE'
                              AND (
                                   (cr.status = 'ACTIVE' AND cr.expires_at > NOW())
                                OR (cr.status = 'PAYMENT_PROTECTED' AND cr.payment_protection_expires_at IS NOT NULL AND cr.payment_protection_expires_at > NOW())
                              );
                            """)) {
                        psSum.setObject(1, storeId);
                        psSum.setObject(2, vId);
                        try (ResultSet rsSum = psSum.executeQuery()) {
                            rsSum.next();
                            reservedQuantity = rsSum.getInt(1);
                        }
                    }

                    int available = stock - reservedQuantity;
                    if (available < requested) {
                        throw new IllegalStateException("INSUFFICIENT_STOCK: variante " + vId + ". Disponible=" + available + ", Solicitado=" + requested);
                    }
                }

                // 10. INSERT ALL: crear todas las inventory_reservations
                for (UUID vId : sortedVariantIds) {
                    int quantity = requestedQuantities.get(vId);
                    try (PreparedStatement psIns = conn.prepareStatement("""
                            INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW() + INTERVAL '10 minutes', NOW(), NOW());
                            """)) {
                        psIns.setObject(1, UUID.randomUUID());
                        psIns.setObject(2, storeId);
                        psIns.setObject(3, capacityReservationId);
                        psIns.setObject(4, vId);
                        psIns.setInt(5, quantity);
                        psIns.executeUpdate();
                    }
                }
            }

            conn.commit();
            return capacityReservationId;
        } catch (Exception e) {
            conn.rollback();
            throw e;
        }
    }

    @Test
    @Order(1)
    @DisplayName("TEST 1: Último stock (stock = 1) bajo concurrencia real entre 2 compradores")
    void test1_lastStockConcurrentCheckout() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(20);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customerA);
            createTestUser(setupConn, customerB);
            createVariant(setupConn, varId, true);
            setStock(setupConn, varId, 1);
            setupConn.commit();
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        Runnable taskA = () -> {
            try (Connection conn = getConnection()) {
                startLatch.await();
                executeCheckoutHold(conn, STORE_ID, customerA, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                        List.of(new VariantItem(varId, 1, true)));
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("INSUFFICIENT_STOCK")) {
                    insufficientStockCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable taskB = () -> {
            try (Connection conn = getConnection()) {
                startLatch.await();
                executeCheckoutHold(conn, STORE_ID, customerB, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                        List.of(new VariantItem(varId, 1, true)));
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("INSUFFICIENT_STOCK")) {
                    insufficientStockCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(taskA);
        executor.submit(taskB);

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactamente 1 checkout debe tener éxito");
        assertEquals(1, insufficientStockCount.get(), "Exactamente 1 checkout debe fallar con INSUFFICIENT_STOCK");

        // Validar reservas vigentes en BD
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) FROM inventory_reservations WHERE variant_id = ? AND status = 'ACTIVE';
                    """)) {
                ps.setObject(1, varId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(1, rs.getInt(1), "Debe quedar exactamente 1 hold vigente de inventario en BD");
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(2)
    @DisplayName("TEST 2: Stock = 5, Customer A qty=4 y Customer B qty=4 simultáneos")
    void test2_partialQuantityConcurrentCheckout() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(21);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customerA);
            createTestUser(setupConn, customerB);
            createVariant(setupConn, varId, true);
            setStock(setupConn, varId, 5);
            setupConn.commit();
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> {
            try (Connection conn = getConnection()) {
                startLatch.await();
                executeCheckoutHold(conn, STORE_ID, customerA, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                        List.of(new VariantItem(varId, 4, true)));
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("INSUFFICIENT_STOCK")) {
                    insufficientStockCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try (Connection conn = getConnection()) {
                startLatch.await();
                executeCheckoutHold(conn, STORE_ID, customerB, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                        List.of(new VariantItem(varId, 4, true)));
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("INSUFFICIENT_STOCK")) {
                    insufficientStockCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Solo 1 checkout puede reservar 4 unidades de las 5 disponibles");
        assertEquals(1, insufficientStockCount.get(), "El segundo debe fallar con INSUFFICIENT_STOCK");
    }

    @Test
    @Order(3)
    @DisplayName("TEST 3: Múltiples variantes [V1,V2] vs [V2,V1] previene deadlocks y reserva limpiamente")
    void test3_multipleVariantsDeadlockPrevention() throws Exception {
        UUID var1 = UUID.randomUUID();
        UUID var2 = UUID.randomUUID();
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(22);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customerA);
            createTestUser(setupConn, customerB);
            createVariant(setupConn, var1, true);
            createVariant(setupConn, var2, true);
            setStock(setupConn, var1, 1);
            setStock(setupConn, var2, 1);
            setupConn.commit();
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientStockCount = new AtomicInteger(0);
        AtomicInteger deadlockCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Customer A solicita [V1, V2]
        executor.submit(() -> {
            try (Connection conn = getConnection()) {
                startLatch.await();
                executeCheckoutHold(conn, STORE_ID, customerA, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                        List.of(new VariantItem(var1, 1, true), new VariantItem(var2, 1, true)));
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("deadlock")) {
                    deadlockCount.incrementAndGet();
                } else if (e.getMessage() != null && e.getMessage().contains("INSUFFICIENT_STOCK")) {
                    insufficientStockCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        });

        // Customer B solicita orden inverso [V2, V1]
        executor.submit(() -> {
            try (Connection conn = getConnection()) {
                startLatch.await();
                executeCheckoutHold(conn, STORE_ID, customerB, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                        List.of(new VariantItem(var2, 1, true), new VariantItem(var1, 1, true)));
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("deadlock")) {
                    deadlockCount.incrementAndGet();
                } else if (e.getMessage() != null && e.getMessage().contains("INSUFFICIENT_STOCK")) {
                    insufficientStockCount.incrementAndGet();
                }
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(0, deadlockCount.get(), "El orden canónico DEBE evitar deadlocks en PostgreSQL");
        assertEquals(1, successCount.get(), "Un checkout obtiene ambos recursos");
        assertEquals(1, insufficientStockCount.get(), "El otro falla limpiamente");
    }

    @Test
    @Order(4)
    @DisplayName("TEST 4: Rollback completo cuando inventario es insuficiente (no queda capacity_reservation)")
    void test4_rollbackOnInsufficientInventory() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(23);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customer);
            createVariant(setupConn, varId, true);
            setStock(setupConn, varId, 0); // Stock 0
            setupConn.commit();
        }

        try (Connection conn = getConnection()) {
            assertThrows(IllegalStateException.class, () ->
                    executeCheckoutHold(conn, STORE_ID, customer, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                            List.of(new VariantItem(varId, 1, true)))
            );
        }

        // Verificar que no quedó capacity_reservation persistida
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) FROM capacity_reservations WHERE user_id = ? AND service_date = ?;
                    """)) {
                ps.setObject(1, customer);
                ps.setObject(2, serviceDate);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "No debe existir capacity_reservation persistida tras rollback");
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(5)
    @DisplayName("TEST 5: Capacidad insuficiente -> no se crea ninguna inventory_reservation")
    void test5_capacityExhaustedNoInventoryReservation() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(24);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customer);
            createVariant(setupConn, varId, true);
            setStock(setupConn, varId, 10);

            // Llenar artificialmente la capacidad del slot (max_capacity = 8 en semilla)
            for (int i = 0; i < 8; i++) {
                UUID dummyUser = UUID.randomUUID();
                createTestUser(setupConn, dummyUser);
                try (PreparedStatement ps = setupConn.prepareStatement("""
                        INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, 'COMMITTED', NOW() + INTERVAL '10 minutes', NOW(), NOW());
                        """)) {
                    ps.setObject(1, UUID.randomUUID());
                    ps.setObject(2, STORE_ID);
                    ps.setObject(3, dummyUser);
                    ps.setObject(4, serviceDate);
                    ps.setObject(5, startTime);
                    ps.setObject(6, endTime);
                    ps.executeUpdate();
                }
            }
            setupConn.commit();
        }

        try (Connection conn = getConnection()) {
            assertThrows(IllegalStateException.class, () ->
                    executeCheckoutHold(conn, STORE_ID, customer, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                            List.of(new VariantItem(varId, 1, true)))
            );
        }

        // Verificar que no quedó inventory_reservation creada
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) FROM inventory_reservations WHERE variant_id = ?;
                    """)) {
                ps.setObject(1, varId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "No debe crearse inventory_reservation si la capacidad falló");
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(6)
    @DisplayName("TEST 6: Expiración lógica sin worker -> reserva vencida deja de consumir disponibilidad")
    void test6_logicalExpirationWithoutWorker() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customerA = UUID.randomUUID();
        UUID customerB = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(25);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customerA);
            createTestUser(setupConn, customerB);
            createVariant(setupConn, varId, true);
            setStock(setupConn, varId, 1);

            // Insertar hold de Customer A que venció hace 10 segundos (status sigue ACTIVE)
            UUID expiredCapId = UUID.randomUUID();
            try (PreparedStatement psCap = setupConn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', NOW() - INTERVAL '10 seconds', NOW() - INTERVAL '11 minutes', NOW());
                    """)) {
                psCap.setObject(1, expiredCapId);
                psCap.setObject(2, STORE_ID);
                psCap.setObject(3, customerA);
                psCap.setObject(4, serviceDate);
                psCap.setObject(5, startTime);
                psCap.setObject(6, endTime);
                psCap.executeUpdate();
            }

            try (PreparedStatement psInv = setupConn.prepareStatement("""
                    INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 1, 'ACTIVE', NOW() - INTERVAL '10 seconds', NOW() - INTERVAL '11 minutes', NOW());
                    """)) {
                psInv.setObject(1, UUID.randomUUID());
                psInv.setObject(2, STORE_ID);
                psInv.setObject(3, expiredCapId);
                psInv.setObject(4, varId);
                psInv.executeUpdate();
            }
            setupConn.commit();
        }

        // Customer B solicita 1 unidad: debe tener éxito inmediatamente porque el hold de A está lógicamente vencido
        try (Connection conn = getConnection()) {
            UUID newHold = executeCheckoutHold(conn, STORE_ID, customerB, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                    List.of(new VariantItem(varId, 1, true)));
            assertNotNull(newHold);
        }
    }

    @Test
    @Order(7)
    @DisplayName("TEST 7: tracksInventory=false sin fila inventories -> checkout permitido y sin inventory_reservation")
    void test7_untrackedVariantCheckoutAllowedWithoutInventory() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(26);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customer);
            createVariant(setupConn, varId, false); // tracksInventory = false, sin fila en inventories
            setupConn.commit();
        }

        try (Connection conn = getConnection()) {
            UUID holdId = executeCheckoutHold(conn, STORE_ID, customer, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                    List.of(new VariantItem(varId, 5, false)));
            assertNotNull(holdId);

            // Verificar que no se creó inventory_reservation
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) FROM inventory_reservations WHERE capacity_reservation_id = ?;
                    """)) {
                ps.setObject(1, holdId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "No debe crearse inventory_reservation para variante untracked");
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(8)
    @DisplayName("TEST 8: tracksInventory=true sin fila inventories -> INVENTORY_NOT_CONFIGURED y rollback total")
    void test8_trackedVariantWithoutInventoryThrowsAndRollbacks() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(27);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customer);
            createVariant(setupConn, varId, true); // tracksInventory = true pero SIN fila en inventories
            setupConn.commit();
        }

        try (Connection conn = getConnection()) {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    executeCheckoutHold(conn, STORE_ID, customer, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                            List.of(new VariantItem(varId, 1, true)))
            );
            assertTrue(ex.getMessage().contains("INVENTORY_NOT_CONFIGURED"));
        }

        // Verificar rollback total de capacity reservation
        try (Connection conn = getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) FROM capacity_reservations WHERE user_id = ? AND service_date = ?;
                    """)) {
                ps.setObject(1, customer);
                ps.setObject(2, serviceDate);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "Rollback completo de capacity reservation ante INVENTORY_NOT_CONFIGURED");
                }
            }
            conn.rollback();
        }
    }

    @Test
    @Order(9)
    @DisplayName("TEST 9: Carrito con variante repetida -> agrupa cantidades y crea exactamente 1 reserva con suma correcta")
    void test9_repeatedVariantGroupedCorrectly() throws Exception {
        UUID varId = UUID.randomUUID();
        UUID customer = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(28);
        LocalTime startTime = LocalTime.of(14, 0);
        LocalTime endTime = LocalTime.of(16, 0);

        try (Connection setupConn = getConnection()) {
            createTestUser(setupConn, customer);
            createVariant(setupConn, varId, true);
            setStock(setupConn, varId, 10);
            setupConn.commit();
        }

        try (Connection conn = getConnection()) {
            // Variante repetida: 2 unidades + 3 unidades = 5 unidades
            UUID holdId = executeCheckoutHold(conn, STORE_ID, customer, CAPACITY_CONFIG_ID, serviceDate, startTime, endTime,
                    List.of(new VariantItem(varId, 2, true), new VariantItem(varId, 3, true)));
            assertNotNull(holdId);

            // Verificar en BD que existe exactamente 1 registro para esta variante con quantity = 5
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT quantity FROM inventory_reservations WHERE capacity_reservation_id = ? AND variant_id = ?;
                    """)) {
                ps.setObject(1, holdId);
                ps.setObject(2, varId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertTrue(rs.next(), "Debe existir registro en inventory_reservations");
                    assertEquals(5, rs.getInt("quantity"), "La cantidad debe ser la suma agrupada (2 + 3 = 5)");
                    assertFalse(rs.next(), "No debe existir más de un registro para la misma variante");
                }
            }
            conn.rollback();
        }
    }
}
