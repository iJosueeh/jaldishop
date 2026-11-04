package com.jaldishop.backend.inventory.infrastructure.persistence;

import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryPostgresIntegrationTest {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    // Tienda y variantes seedeadas en V4
    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID TRACKED_VARIANT_1 = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302"); // low_stock_threshold=2

    @BeforeAll
    static void initDatabase() throws SQLException {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        dbUrl = config.url();
        dbUser = config.username();
        dbPassword = config.password();

        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, dbUser, dbPassword)
                .locations("classpath:db/migration")
                .validateOnMigrate(config.validateOnMigrate())
                .load();
        flyway.migrate();
    }

    @Test
    @DisplayName("Verifica proyecciones nativas de inventario por tienda")
    void testFindAllVariantInventoriesByStoreId() throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            var stmt = conn.prepareStatement("""
                SELECT
                    pv.id AS variant_id,
                    p.name AS product_name,
                    i.quantity AS physical_quantity,
                    i.low_stock_threshold AS low_stock_threshold
                FROM product_variants pv
                JOIN products p ON pv.product_id = p.id
                LEFT JOIN inventories i ON pv.id = i.variant_id
                WHERE pv.store_id = ?
                """);
            stmt.setObject(1, STORE_ID);
            var rs = stmt.executeQuery();

            int count = 0;
            boolean foundTracked1 = false;
            while (rs.next()) {
                count++;
                UUID vId = (UUID) rs.getObject("variant_id");
                if (TRACKED_VARIANT_1.equals(vId)) {
                    foundTracked1 = true;
                    assertThat(rs.getInt("physical_quantity")).isGreaterThanOrEqualTo(0);
                }
            }
            assertThat(count).isGreaterThan(0);
            assertThat(foundTracked1).isTrue();
        }
    }

    @Test
    @DisplayName("Verifica consulta de stock bajo: solo variantes donde physicalQuantity <= lowStockThreshold")
    void testLowStockQuery() throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            try {
                // Ajustar temporalmente para que cumpla condición de stock bajo
                var updateStmt = conn.prepareStatement("UPDATE inventories SET quantity = 1 WHERE variant_id = ?");
                updateStmt.setObject(1, TRACKED_VARIANT_1);
                updateStmt.executeUpdate();

                var stmt = conn.prepareStatement("""
                    SELECT pv.id AS variant_id, i.quantity, i.low_stock_threshold
                    FROM product_variants pv
                    JOIN inventories i ON pv.id = i.variant_id
                    WHERE pv.store_id = ?
                      AND pv.tracks_inventory = true
                      AND i.low_stock_threshold IS NOT NULL
                      AND i.quantity <= i.low_stock_threshold
                    """);
                stmt.setObject(1, STORE_ID);
                var rs = stmt.executeQuery();

                boolean foundLowStockVariant = false;
                while (rs.next()) {
                    int qty = rs.getInt("quantity");
                    int threshold = rs.getInt("low_stock_threshold");
                    assertThat(qty).isLessThanOrEqualTo(threshold);
                    if (TRACKED_VARIANT_1.equals(rs.getObject("variant_id"))) {
                        foundLowStockVariant = true;
                    }
                }
                assertThat(foundLowStockVariant).isTrue();
            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    @DisplayName("Verifica upsert nativo seguro: inserta o actualiza preservando threshold")
    void testNativeUpsert() throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            conn.setAutoCommit(false);
            try {
                // Obtener threshold actual
                int originalThreshold;
                try (var q = conn.prepareStatement("SELECT low_stock_threshold FROM inventories WHERE variant_id = ?")) {
                    q.setObject(1, TRACKED_VARIANT_1);
                    var rs = q.executeQuery();
                    assertThat(rs.next()).isTrue();
                    originalThreshold = rs.getInt("low_stock_threshold");
                }

                // Modificar quantity con upsert nativo
                var upsertStmt = conn.prepareStatement("""
                    INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at)
                    VALUES (?, 42, NULL, NOW())
                    ON CONFLICT (variant_id) DO UPDATE
                    SET quantity = EXCLUDED.quantity,
                        updated_at = EXCLUDED.updated_at
                    """);
                upsertStmt.setObject(1, TRACKED_VARIANT_1);
                upsertStmt.executeUpdate();

                // Verificar que quantity cambió a 42 pero low_stock_threshold se preservó exactamente
                var queryStmt = conn.prepareStatement("SELECT quantity, low_stock_threshold FROM inventories WHERE variant_id = ?");
                queryStmt.setObject(1, TRACKED_VARIANT_1);
                var rs = queryStmt.executeQuery();
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("quantity")).isEqualTo(42);
                assertThat(rs.getInt("low_stock_threshold")).isEqualTo(originalThreshold); // PRESERVADO
            } finally {
                conn.rollback(); // rollback para no ensuciar datos
            }
        }
    }
}
