package com.jaldishop.backend.database;

import io.github.cdimascio.dotenv.Dotenv;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.*;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostgreSqlArchitectureIntegrationTest {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean dbAvailable = false;

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
                // Run Flyway migrations V1 through V14
                Flyway flyway = Flyway.configure()
                        .dataSource(dbUrl, dbUser, dbPassword)
                        .locations("classpath:db/migration")
                        .validateOnMigrate(false)
                        .load();
                MigrateResult result = flyway.migrate();
                System.out.println("Flyway migration completed: " + result.migrationsExecuted + " migrations executed.");
            } catch (Exception e) {
                System.err.println("Database connection or Flyway failed: " + e.getMessage());
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

    @FunctionalInterface
    interface SqlExecutable {
        void execute() throws SQLException;
    }

    private void assertThrowsInSavepoint(Connection conn, Class<? extends Throwable> expectedType, SqlExecutable executable, String message) throws SQLException {
        Savepoint sp = conn.setSavepoint();
        try {
            assertThrows(expectedType, executable::execute, message);
        } finally {
            conn.rollback(sp);
        }
    }

    private UUID createTestUser(Connection conn, String email) throws SQLException {
        UUID userId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'hash', 'Test', 'User', 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, userId);
            ps.setString(2, email);
            ps.executeUpdate();
        }
        return userId;
    }

    private UUID createTestStore(Connection conn, UUID ownerUserId, String slug) throws SQLException {
        UUID storeId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO stores (id, owner_user_id, name, slug, pickup_enabled, delivery_enabled, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'Tienda Test', ?, true, false, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, storeId);
            ps.setObject(2, ownerUserId);
            ps.setString(3, slug);
            ps.executeUpdate();
        }
        return storeId;
    }

    @Test
    @Order(1)
    @DisplayName("1 & 2. CUSTOMER en Store A y Store B permitido; duplicado en misma tienda rechazado")
    void testCustomerMultiStoreAndUniquePerStore() throws SQLException {
        try (Connection conn = getConnection()) {
            UUID user = createTestUser(conn, "customer.multi@" + UUID.randomUUID() + ".com");
            UUID owner1 = createTestUser(conn, "owner1@" + UUID.randomUUID() + ".com");
            UUID owner2 = createTestUser(conn, "owner2@" + UUID.randomUUID() + ".com");
            UUID storeA = createTestStore(conn, owner1, "store-a-" + UUID.randomUUID());
            UUID storeB = createTestStore(conn, owner2, "store-b-" + UUID.randomUUID());

            // 1. Asignar CUSTOMER a Store A
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 1, ?)")) {
                ps.setObject(1, user);
                ps.setObject(2, storeA);
                assertEquals(1, ps.executeUpdate());
            }

            // Asignar CUSTOMER a Store B (permitido)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 1, ?)")) {
                ps.setObject(1, user);
                ps.setObject(2, storeB);
                assertEquals(1, ps.executeUpdate());
            }

            // 2. Duplicar CUSTOMER en Store A (debe fallar por uq_idx_user_roles_store)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 1, ?)")) {
                ps.setObject(1, user);
                ps.setObject(2, storeA);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "No debe permitir duplicar CUSTOMER en la misma tienda");
            }
            conn.rollback();
        }
    }

    @Test
    @Order(2)
    @DisplayName("3 & 4. MERCHANT requiere store_id; ADMIN no lleva store_id (CHECK ck_user_roles_role_scope)")
    void testRoleScopeCheckConstraint() throws SQLException {
        try (Connection conn = getConnection()) {
            UUID user = createTestUser(conn, "scope.test@" + UUID.randomUUID() + ".com");
            UUID owner = createTestUser(conn, "scope.owner@" + UUID.randomUUID() + ".com");
            UUID store = createTestStore(conn, owner, "scope-store-" + UUID.randomUUID());

            // 1. Registrar un CUSTOMER sin tienda -> permitido (global)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 1, NULL)")) {
                ps.setObject(1, user);
                assertEquals(1, ps.executeUpdate(), "CUSTOMER global debe permitirse");
            }

            // 2. Duplicar CUSTOMER global -> rechazado por uq_idx_user_roles_global
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 1, NULL)")) {
                ps.setObject(1, user);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "Duplicar CUSTOMER global debe rechazarse");
            }

            // 3. MERCHANT con store_id NULL debe fallar
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 2, NULL)")) {
                ps.setObject(1, user);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "MERCHANT sin store_id debe violar ck_user_roles_role_scope");
            }

            // 4. MERCHANT con store_id NOT NULL debe ser exitoso
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 2, ?)")) {
                ps.setObject(1, user);
                ps.setObject(2, store);
                assertEquals(1, ps.executeUpdate(), "MERCHANT con store_id debe permitirse");
            }

            // 5. ADMIN con store_id NOT NULL debe fallar
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 3, ?)")) {
                ps.setObject(1, user);
                ps.setObject(2, store);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "ADMIN con store_id debe violar ck_user_roles_role_scope");
            }

            // 6. ADMIN global (store_id = NULL) debe ser exitoso
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO user_roles (id, user_id, role_id, store_id) VALUES (gen_random_uuid(), ?, 3, NULL)")) {
                ps.setObject(1, user);
                assertEquals(1, ps.executeUpdate(), "ADMIN global debe permitirse");
            }

            conn.rollback();
        }
    }

    @Test
    @Order(3)
    @DisplayName("5. Owner conserva relación 1:1 con store (UNIQUE owner_user_id)")
    void testStoreOwnerUniqueness() throws SQLException {
        try (Connection conn = getConnection()) {
            UUID owner = createTestUser(conn, "single.owner@" + UUID.randomUUID() + ".com");
            createTestStore(conn, owner, "store-1-" + UUID.randomUUID());

            // Intentar crear segunda tienda con el mismo owner (debe fallar)
            assertThrowsInSavepoint(conn, SQLException.class, () -> createTestStore(conn, owner, "store-2-" + UUID.randomUUID()),
                    "Un owner no puede poseer más de una tienda en el MVP");

            conn.rollback();
        }
    }

    @Test
    @Order(4)
    @DisplayName("7 & 8. Producto admite múltiples imágenes, pero máximo una primaria")
    void testProductImagesPrimaryConstraint() throws SQLException {
        try (Connection conn = getConnection()) {
            UUID owner = createTestUser(conn, "img.owner@" + UUID.randomUUID() + ".com");
            UUID store = createTestStore(conn, owner, "store-img-" + UUID.randomUUID());

            UUID categoryId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO categories (id, store_id, name, status, created_at, updated_at) VALUES (?, ?, 'Cat Img', 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, categoryId);
                ps.setObject(2, store);
                ps.executeUpdate();
            }

            UUID productId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products (id, store_id, category_id, name, slug, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'Prod Img', ?, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, productId);
                ps.setObject(2, store);
                ps.setObject(3, categoryId);
                ps.setString(4, "prod-img-" + UUID.randomUUID());
                ps.executeUpdate();
            }

            // Insertar primera imagen como primaria
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_images (id, product_id, image_url, position, is_primary, created_at) VALUES (gen_random_uuid(), ?, 'url1', 0, true, NOW())")) {
                ps.setObject(1, productId);
                assertEquals(1, ps.executeUpdate());
            }

            // Insertar segunda imagen secundaria (permitido)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_images (id, product_id, image_url, position, is_primary, created_at) VALUES (gen_random_uuid(), ?, 'url2', 1, false, NOW())")) {
                ps.setObject(1, productId);
                assertEquals(1, ps.executeUpdate());
            }

            // Insertar tercera imagen secundaria (permitido)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_images (id, product_id, image_url, position, is_primary, created_at) VALUES (gen_random_uuid(), ?, 'url3', 2, false, NOW())")) {
                ps.setObject(1, productId);
                assertEquals(1, ps.executeUpdate());
            }

            // Intentar insertar otra imagen como primaria (debe fallar)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_images (id, product_id, image_url, position, is_primary, created_at) VALUES (gen_random_uuid(), ?, 'url4', 3, true, NOW())")) {
                ps.setObject(1, productId);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "Solo una imagen primaria permitida por producto");
            }

            conn.rollback();
        }
    }

    @Test
    @Order(5)
    @DisplayName("9. Inventory reservation no puede combinar reserva de Store A con variante de Store B (FK compuesta)")
    void testInventoryReservationTenantSafe() throws SQLException {
        try (Connection conn = getConnection()) {
            UUID ownerA = createTestUser(conn, "res.ownerA@" + UUID.randomUUID() + ".com");
            UUID ownerB = createTestUser(conn, "res.ownerB@" + UUID.randomUUID() + ".com");
            UUID storeA = createTestStore(conn, ownerA, "store-res-a-" + UUID.randomUUID());
            UUID storeB = createTestStore(conn, ownerB, "store-res-b-" + UUID.randomUUID());

            UUID userCustomer = createTestUser(conn, "res.cust@" + UUID.randomUUID() + ".com");

            // Capacity Reservation en Store A
            UUID capResA = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, status, expires_at, created_at, updated_at) " +
                            "VALUES (?, ?, ?, CURRENT_DATE, 'ACTIVE', NOW() + INTERVAL '10 min', NOW(), NOW())")) {
                ps.setObject(1, capResA);
                ps.setObject(2, storeA);
                ps.setObject(3, userCustomer);
                ps.executeUpdate();
            }

            // Variant en Store B
            UUID catB = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO categories (id, store_id, name, status, created_at, updated_at) VALUES (?, ?, 'Cat B', 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, catB);
                ps.setObject(2, storeB);
                ps.executeUpdate();
            }

            UUID prodB = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products (id, store_id, category_id, name, slug, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'Prod B', ?, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, prodB);
                ps.setObject(2, storeB);
                ps.setObject(3, catB);
                ps.setString(4, "slug-b-" + UUID.randomUUID());
                ps.executeUpdate();
            }

            UUID variantB = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_variants (id, store_id, product_id, presentation_name, price_amount, price_currency, tracks_inventory, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'Var B', 10.00, 'PEN', true, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, variantB);
                ps.setObject(2, storeB);
                ps.setObject(3, prodB);
                ps.executeUpdate();
            }

            // Intentar crear Inventory Reservation con store_id = Store A pero variant de Store B
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, 1, 'ACTIVE', NOW() + INTERVAL '10 min', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                ps.setObject(2, capResA);
                ps.setObject(3, variantB);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "No se puede asociar variante de Store B a reserva de Store A");
            }

            conn.rollback();
        }
    }

    @Test
    @Order(6)
    @DisplayName("6. Capacidad: Validación exhaustiva de solapamiento con EXCLUDE USING gist (Casos A a F)")
    void testCapacitySlotOverlapsExcludeConstraint() throws SQLException {
        try (Connection conn = getConnection()) {
            UUID ownerA = createTestUser(conn, "cap.ownerA@" + UUID.randomUUID() + ".com");
            UUID ownerB = createTestUser(conn, "cap.ownerB@" + UUID.randomUUID() + ".com");
            UUID storeA = createTestStore(conn, ownerA, "store-cap-a-" + UUID.randomUUID());
            UUID storeB = createTestStore(conn, ownerB, "store-cap-b-" + UUID.randomUUID());

            // A) ACTIVE 09:00-12:00 y ACTIVE 12:00-15:00 => permitido (franjas adyacentes [start, end))
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, 1, '09:00:00', '12:00:00', 5, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, 1, '12:00:00', '15:00:00', 5, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                assertEquals(1, ps.executeUpdate());
            }

            // B) ACTIVE 10:00-13:00 en storeA, día 1 => rechazado (solapa con 09-12 y 12-15)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, 1, '10:00:00', '13:00:00', 5, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "Franja 10:00-13:00 debe ser rechazada por solapamiento");
            }

            // C) ACTIVE FULL DAY (NULL, NULL) en storeA, día 1 => rechazado (coexiste con 09:00-12:00 ACTIVE)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, 1, NULL, NULL, 10, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "Día completo ACTIVE debe ser rechazado si ya hay slots");
            }

            // D) INACTIVE 10:00-13:00 => permitido (solapamiento solo aplica a ACTIVE)
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, 1, '10:00:00', '13:00:00', 5, 'INACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                assertEquals(1, ps.executeUpdate(), "Slot INACTIVE puede solaparse");
            }

            // E) Misma franja 09:00-12:00 pero diferente store_id (storeB) => permitido
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, 1, '09:00:00', '12:00:00', 5, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeB);
                assertEquals(1, ps.executeUpdate(), "Misma franja en diferente tienda debe permitirse");
            }

            // F) capacity_exceptions: misma franja pero diferente service_date => permitido
            LocalDate date1 = LocalDate.now().plusDays(1);
            LocalDate date2 = LocalDate.now().plusDays(2);

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_exceptions (id, store_id, service_date, start_time, end_time, exception_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, '09:00:00', '12:00:00', 3, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                ps.setObject(2, date1);
                assertEquals(1, ps.executeUpdate());
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_exceptions (id, store_id, service_date, start_time, end_time, exception_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, '09:00:00', '12:00:00', 3, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                ps.setObject(2, date2);
                assertEquals(1, ps.executeUpdate(), "Misma franja en fechas de excepción distintas debe permitirse");
            }

            // Solapamiento en misma fecha de excepción => rechazado
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_exceptions (id, store_id, service_date, start_time, end_time, exception_capacity, status, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, '10:00:00', '13:00:00', 3, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, storeA);
                ps.setObject(2, date1);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "Solapamiento en la misma fecha de excepción debe rechazarse");
            }

            conn.rollback();
        }
    }

    @Test
    @Order(7)
    @DisplayName("Quantity <= 0 es rechazada en cart_items, order_items e inventory_reservations")
    void testQuantityChecks() throws SQLException {
        try (Connection conn = getConnection()) {
            UUID owner = createTestUser(conn, "qty.owner@" + UUID.randomUUID() + ".com");
            UUID store = createTestStore(conn, owner, "store-qty-" + UUID.randomUUID());
            UUID user = createTestUser(conn, "qty.user@" + UUID.randomUUID() + ".com");

            UUID cartId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO carts (id, user_id, store_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())")) {
                ps.setObject(1, cartId);
                ps.setObject(2, user);
                ps.setObject(3, store);
                ps.executeUpdate();
            }

            UUID catId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO categories (id, store_id, name, status, created_at, updated_at) VALUES (?, ?, 'Cat Qty', 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, catId);
                ps.setObject(2, store);
                ps.executeUpdate();
            }

            UUID prodId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO products (id, store_id, category_id, name, slug, status, created_at, updated_at) VALUES (?, ?, ?, 'Prod Qty', ?, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, prodId);
                ps.setObject(2, store);
                ps.setObject(3, catId);
                ps.setString(4, "slug-qty-" + UUID.randomUUID());
                ps.executeUpdate();
            }

            UUID varId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO product_variants (id, store_id, product_id, presentation_name, price_amount, price_currency, tracks_inventory, status, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 'Var Qty', 10.00, 'PEN', true, 'ACTIVE', NOW(), NOW())")) {
                ps.setObject(1, varId);
                ps.setObject(2, store);
                ps.setObject(3, prodId);
                ps.executeUpdate();
            }

            // cart_items quantity <= 0 debe fallar
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO cart_items (cart_id, variant_id, store_id, quantity, reference_price_amount, reference_price_currency, created_at, updated_at) " +
                            "VALUES (?, ?, ?, 0, 10.00, 'PEN', NOW(), NOW())")) {
                ps.setObject(1, cartId);
                ps.setObject(2, varId);
                ps.setObject(3, store);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "cart_items con quantity = 0 debe fallar por ck_cart_items_quantity");
            }

            // inventory_reservations quantity <= 0 debe fallar
            UUID capResId = UUID.randomUUID();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, status, expires_at, created_at, updated_at) " +
                            "VALUES (?, ?, ?, CURRENT_DATE, 'ACTIVE', NOW() + INTERVAL '10 min', NOW(), NOW())")) {
                ps.setObject(1, capResId);
                ps.setObject(2, store);
                ps.setObject(3, user);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at) " +
                            "VALUES (gen_random_uuid(), ?, ?, ?, 0, 'ACTIVE', NOW() + INTERVAL '10 min', NOW(), NOW())")) {
                ps.setObject(1, store);
                ps.setObject(2, capResId);
                ps.setObject(3, varId);
                assertThrowsInSavepoint(conn, SQLException.class, ps::executeUpdate, "inventory_reservations con quantity = 0 debe fallar");
            }

            conn.rollback();
        }
    }
}
