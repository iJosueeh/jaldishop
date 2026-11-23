package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.mapper.InventoryResponseMapper;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InventoryEquivalenceAndPerformancePostgresIntegrationTest {

    @Autowired
    private GetStoreInventoryService getStoreInventoryService;

    @Autowired
    private GetLowStockInventoryService getLowStockInventoryService;

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    @Autowired
    private InventoryResponseMapper responseMapper;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private DataSource dataSource;

    private NamedParameterJdbcTemplate jdbcTemplate;
    private Statistics hibernateStatistics;

    private static final UUID SEEDED_CATEGORY_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf101");
    private static final UUID SEEDED_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9f3");

    // Identificadores de prueba aislados
    private UUID storeAlpha;
    private UUID storeBeta;
    private UUID storeEmpty;
    private UUID storeSingle;
    private UUID storeMulti;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        registry.add("spring.datasource.url", config::url);
        registry.add("spring.datasource.username", config::username);
        registry.add("spring.datasource.password", config::password);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @BeforeAll
    void migrateAndSetup() throws SQLException {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        Flyway flyway = Flyway.configure()
                .dataSource(config.url(), config.username(), config.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(config.validateOnMigrate())
                .load();
        flyway.migrate();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        this.hibernateStatistics = sessionFactory.getStatistics();
        this.hibernateStatistics.setStatisticsEnabled(true);
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        seedScenarios();
    }

    private void seedScenarios() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Tienda sin variantes
            storeEmpty = insertStore(conn, "store-empty-" + UUID.randomUUID());

            // 2. Tienda con 1 variante
            storeSingle = insertStore(conn, "store-single-" + UUID.randomUUID());
            UUID catSingle = insertCategory(conn, storeSingle, "Cat Single");
            UUID prodSingle = insertProduct(conn, storeSingle, catSingle, "Prod Single");
            UUID varSingle = insertVariant(conn, storeSingle, prodSingle, "Var 1", "SKU-S1", true);
            insertInventory(conn, varSingle, 15, 3);

            // 3. Tienda con múltiples variantes para aislamientos y combinaciones complejas
            storeAlpha = insertStore(conn, "store-alpha-" + UUID.randomUUID());
            UUID catAlpha = insertCategory(conn, storeAlpha, "Cat Alpha");
            UUID prodAlpha1 = insertProduct(conn, storeAlpha, catAlpha, "Pastel de Chocolate");
            UUID prodAlpha2 = insertProduct(conn, storeAlpha, catAlpha, "Café Latte");

            // V1: Configurada, stock 10, umbral 5, con reserva activa vigente (qty 3) -> available=7, lowStock=false
            UUID v1 = insertVariant(conn, storeAlpha, prodAlpha1, "Porción V1", "SKU-A1", true);
            insertInventory(conn, v1, 10, 5);
            createReservation(conn, storeAlpha, v1, 3, "ACTIVE", "ACTIVE", 15, 0);

            // V2: Configurada, stock 4, umbral 5 (condición stock bajo!) con reserva activa (qty 1) -> available=3, lowStock=true
            UUID v2 = insertVariant(conn, storeAlpha, prodAlpha1, "Entero V2", "SKU-A2", true);
            insertInventory(conn, v2, 4, 5);
            createReservation(conn, storeAlpha, v2, 1, "ACTIVE", "ACTIVE", 20, 0);

            // V3: Configurada, stock físico agotado (0), umbral 0 -> available=0, lowStock=true
            UUID v3 = insertVariant(conn, storeAlpha, prodAlpha1, "Mini V3", "SKU-A3", true);
            insertInventory(conn, v3, 0, 0);

            // V4: Configurada, stock 8, umbral NULL -> lowStock=false siempre
            UUID v4 = insertVariant(conn, storeAlpha, prodAlpha2, "Mediano V4", "SKU-A4", true);
            insertInventory(conn, v4, 8, null);

            // V5: Con tracksInventory=true pero SIN fila en inventories -> NOT_CONFIGURED
            UUID v5 = insertVariant(conn, storeAlpha, prodAlpha2, "Grande V5", "SKU-A5", true);

            // V6: Con tracksInventory=false -> NOT_TRACKED
            UUID v6 = insertVariant(conn, storeAlpha, prodAlpha2, "Gigante V6", "SKU-A6", false);

            // V7: Configurada, stock 20, con reservas expiradas y confirmadas (no deben sumarse)
            UUID v7 = insertVariant(conn, storeAlpha, prodAlpha2, "Especial V7", "SKU-A7", true);
            insertInventory(conn, v7, 20, 2);
            // Reserva con capacidad vencida en el pasado (-10 min)
            createReservation(conn, storeAlpha, v7, 5, "ACTIVE", "ACTIVE", -10, 0);
            // Reserva confirmada (COMMITTED)
            createReservation(conn, storeAlpha, v7, 4, "COMMITTED", "COMMITTED", 15, 0);
            // Reserva liberada (RELEASED)
            createReservation(conn, storeAlpha, v7, 2, "RELEASED", "RELEASED", 15, 0);

            // V8: Con reserva en PAYMENT_PROTECTED vigente (+10 min) -> debe sumarse!
            UUID v8 = insertVariant(conn, storeAlpha, prodAlpha2, "Premium V8", "SKU-A8", true);
            insertInventory(conn, v8, 12, 4);
            createReservation(conn, storeAlpha, v8, 4, "ACTIVE", "PAYMENT_PROTECTED", 0, 10);

            // 4. Tienda Beta (para validar aislamiento multitienda)
            storeBeta = insertStore(conn, "store-beta-" + UUID.randomUUID());
            UUID catBeta = insertCategory(conn, storeBeta, "Cat Beta");
            UUID prodBeta = insertProduct(conn, storeBeta, catBeta, "Té Verde");
            UUID vBeta = insertVariant(conn, storeBeta, prodBeta, "Caja Beta", "SKU-B1", true);
            insertInventory(conn, vBeta, 50, 10);
            // Reserva masiva en tienda Beta (qty 40) que NUNCA debe afectar a Alpha
            createReservation(conn, storeBeta, vBeta, 40, "ACTIVE", "ACTIVE", 30, 0);

            // 5. Tienda con múltiples variantes homogéneas para benchmarking de N+1 (10 variantes)
            storeMulti = insertStore(conn, "store-multi-" + UUID.randomUUID());
            UUID catMulti = insertCategory(conn, storeMulti, "Cat Multi");
            UUID prodMulti = insertProduct(conn, storeMulti, catMulti, "Pack Galletas");
            for (int i = 1; i <= 10; i++) {
                UUID vm = insertVariant(conn, storeMulti, prodMulti, "Formato " + i, "SKU-M" + i, true);
                insertInventory(conn, vm, 20 + i, 5);
                if (i % 2 == 0) {
                    createReservation(conn, storeMulti, vm, 2, "ACTIVE", "ACTIVE", 15, 0);
                }
            }

            conn.commit();
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

    private UUID insertStore(Connection conn, String slug) throws SQLException {
        UUID id = UUID.randomUUID();
        UUID ownerId = createTestUser(conn, "owner-" + id + "@test.com");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO stores (id, owner_user_id, name, slug, pickup_enabled, delivery_enabled, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'Store Test', ?, true, false, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, id);
            ps.setObject(2, ownerId);
            ps.setString(3, slug);
            ps.executeUpdate();
        }
        return id;
    }

    private UUID insertCategory(Connection conn, UUID storeId, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO categories (id, store_id, name, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, id);
            ps.setObject(2, storeId);
            ps.setString(3, name);
            ps.executeUpdate();
        }
        return id;
    }

    private UUID insertProduct(Connection conn, UUID storeId, UUID categoryId, String name) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO products (id, store_id, category_id, name, slug, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, id);
            ps.setObject(2, storeId);
            ps.setObject(3, categoryId);
            ps.setString(4, name);
            ps.setString(5, "slug-" + id);
            ps.executeUpdate();
        }
        return id;
    }

    private UUID insertVariant(Connection conn, UUID storeId, UUID productId, String name, String sku, boolean tracks) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO product_variants (id, store_id, product_id, presentation_name, sku, price_amount, price_currency, tracks_inventory, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 10.00, 'PEN', ?, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, id);
            ps.setObject(2, storeId);
            ps.setObject(3, productId);
            ps.setString(4, name);
            ps.setString(5, sku);
            ps.setBoolean(6, tracks);
            ps.executeUpdate();
        }
        return id;
    }

    private void insertInventory(Connection conn, UUID variantId, int qty, Integer threshold) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at) " +
                        "VALUES (?, ?, ?, NOW())")) {
            ps.setObject(1, variantId);
            ps.setInt(2, qty);
            if (threshold != null) {
                ps.setInt(3, threshold);
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    private void createReservation(Connection conn, UUID storeId, UUID variantId, int qty,
                                   String invStatus, String capStatus, int expiresMinutes, int protectionMinutes) throws SQLException {
        UUID capId = UUID.randomUUID();
        UUID invResId = UUID.randomUUID();

        Instant now = Instant.now();
        Instant capExpiresAt = now.plusSeconds(expiresMinutes * 60L);
        Instant protectionExpiresAt = protectionMinutes > 0 ? now.plusSeconds(protectionMinutes * 60L) : null;

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, expires_at, payment_protection_expires_at, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, CURRENT_DATE, '10:00:00', '11:00:00', ?, ?, ?, NOW(), NOW())")) {
            ps.setObject(1, capId);
            ps.setObject(2, storeId);
            ps.setObject(3, SEEDED_USER_ID);
            ps.setObject(4, java.sql.Timestamp.from(capExpiresAt));
            if (protectionExpiresAt != null) {
                ps.setObject(5, java.sql.Timestamp.from(protectionExpiresAt));
            } else {
                ps.setNull(5, java.sql.Types.TIMESTAMP);
            }
            ps.setString(6, capStatus);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
            ps.setObject(1, invResId);
            ps.setObject(2, storeId);
            ps.setObject(3, capId);
            ps.setObject(4, variantId);
            ps.setInt(5, qty);
            ps.setString(6, invStatus);
            ps.setObject(7, java.sql.Timestamp.from(capExpiresAt));
            ps.executeUpdate();
        }
    }

    /**
     * Simulación de la futura consulta agrupada candidata para Fase 3B (Batch Aggregation).
     * Ejecuta exactamente 2 consultas: 1 para las proyecciones y 1 para las reservas activas agrupadas.
     */
    private List<InventoryResponse> executeCandidateBatchService(UUID storeId) {
        Instant now = Instant.now();
        List<StoreVariantInventoryProjection> projections =
                inventoryJpaRepository.findAllVariantInventoriesByStoreId(storeId);

        if (projections.isEmpty()) {
            return List.of();
        }

        // Consulta agregada por lote agrupada por variant_id
        String batchSql = """
                SELECT ir.variant_id AS variant_id, COALESCE(SUM(ir.quantity), 0) AS reserved_quantity
                FROM inventory_reservations ir
                JOIN capacity_reservations cr ON ir.capacity_reservation_id = cr.id AND ir.store_id = cr.store_id
                WHERE ir.store_id = :storeId
                  AND ir.status = 'ACTIVE'
                  AND (
                       (cr.status = 'ACTIVE' AND cr.expires_at > :now)
                    OR (cr.status = 'PAYMENT_PROTECTED' AND cr.payment_protection_expires_at IS NOT NULL AND cr.payment_protection_expires_at > :now)
                  )
                GROUP BY ir.variant_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("storeId", storeId)
                .addValue("now", java.sql.Timestamp.from(now));

        Map<UUID, Integer> reservedMap = new HashMap<>();
        jdbcTemplate.query(batchSql, params, rs -> {
            reservedMap.put((UUID) rs.getObject("variant_id"), rs.getInt("reserved_quantity"));
        });

        return projections.stream()
                .map(projection -> {
                    int reserved = 0;
                    if (projection.isTracksInventory() && projection.getPhysicalQuantity() != null) {
                        reserved = reservedMap.getOrDefault(projection.getVariantId(), 0);
                    }
                    return responseMapper.toResponse(projection, reserved);
                })
                .toList();
    }

    // =========================================================================
    // PRUEBAS DE EQUIVALENCIA FUNCIONAL (Paso 4: 10 Casos Cubiertos)
    // =========================================================================

    @Test
    @DisplayName("Paso 4.1: Tienda sin variantes retorna lista vacía idéntica en ambos métodos")
    void testEquivalenceEmptyStore() {
        List<InventoryResponse> current = getStoreInventoryService.execute(storeEmpty);
        List<InventoryResponse> candidate = executeCandidateBatchService(storeEmpty);

        assertThat(current).isEmpty();
        assertThat(candidate).isEmpty();
    }

    @Test
    @DisplayName("Paso 4.2: Tienda con una sola variante retorna exactamente los mismos campos")
    void testEquivalenceSingleVariant() {
        List<InventoryResponse> current = getStoreInventoryService.execute(storeSingle);
        List<InventoryResponse> candidate = executeCandidateBatchService(storeSingle);

        assertThat(current).hasSize(1);
        assertThat(candidate).usingRecursiveComparison().isEqualTo(current);
        assertThat(candidate.get(0).inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(candidate.get(0).physicalQuantity()).isEqualTo(15);
        assertThat(candidate.get(0).availableQuantity()).isEqualTo(15);
        assertThat(candidate.get(0).reservedQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("Paso 4.3 a 4.9: Tienda con múltiples variantes, reservas vigentes, expiradas, umbrales y estados")
    void testEquivalenceComplexStoreAlpha() {
        List<InventoryResponse> current = getStoreInventoryService.execute(storeAlpha);
        List<InventoryResponse> candidate = executeCandidateBatchService(storeAlpha);

        assertThat(current).hasSize(8);
        assertThat(candidate).usingRecursiveComparison().isEqualTo(current);

        // Verificaciones semánticas caso a caso en storeAlpha:
        Map<String, InventoryResponse> map = new HashMap<>();
        candidate.forEach(item -> map.put(item.sku(), item));

        // V1: Configurada con reserva activa vigente de 3
        InventoryResponse r1 = map.get("SKU-A1");
        assertThat(r1.inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(r1.physicalQuantity()).isEqualTo(10);
        assertThat(r1.reservedQuantity()).isEqualTo(3);
        assertThat(r1.availableQuantity()).isEqualTo(7);
        assertThat(r1.isLowStock()).isFalse();

        // V2: Configurada con stock bajo (physical 4 <= threshold 5) y reserva de 1
        InventoryResponse r2 = map.get("SKU-A2");
        assertThat(r2.inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(r2.physicalQuantity()).isEqualTo(4);
        assertThat(r2.reservedQuantity()).isEqualTo(1);
        assertThat(r2.availableQuantity()).isEqualTo(3);
        assertThat(r2.isLowStock()).isTrue();

        // V3: Stock físico agotado (0) y umbral 0 -> available 0, lowStock true
        InventoryResponse r3 = map.get("SKU-A3");
        assertThat(r3.inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(r3.physicalQuantity()).isEqualTo(0);
        assertThat(r3.reservedQuantity()).isEqualTo(0);
        assertThat(r3.availableQuantity()).isEqualTo(0);
        assertThat(r3.isLowStock()).isTrue();

        // V4: Umbral NULL -> isLowStock false
        InventoryResponse r4 = map.get("SKU-A4");
        assertThat(r4.inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(r4.physicalQuantity()).isEqualTo(8);
        assertThat(r4.lowStockThreshold()).isNull();
        assertThat(r4.isLowStock()).isFalse();

        // V5: tracksInventory=true sin fila de inventories -> NOT_CONFIGURED
        InventoryResponse r5 = map.get("SKU-A5");
        assertThat(r5.inventoryStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(r5.physicalQuantity()).isNull();
        assertThat(r5.reservedQuantity()).isNull();
        assertThat(r5.availableQuantity()).isNull();
        assertThat(r5.isLowStock()).isFalse();

        // V6: tracksInventory=false -> NOT_TRACKED
        InventoryResponse r6 = map.get("SKU-A6");
        assertThat(r6.inventoryStatus()).isEqualTo("NOT_TRACKED");
        assertThat(r6.physicalQuantity()).isNull();
        assertThat(r6.reservedQuantity()).isNull();
        assertThat(r6.availableQuantity()).isNull();

        // V7: Reservas expiradas (-10m), confirmadas (COMMITTED) y liberadas (RELEASED) no suman
        InventoryResponse r7 = map.get("SKU-A7");
        assertThat(r7.inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(r7.physicalQuantity()).isEqualTo(20);
        assertThat(r7.reservedQuantity()).isEqualTo(0); // ninguna reserva computa
        assertThat(r7.availableQuantity()).isEqualTo(20);

        // V8: Reserva en PAYMENT_PROTECTED vigente (+10m) -> debe sumarse (qty 4)
        InventoryResponse r8 = map.get("SKU-A8");
        assertThat(r8.inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(r8.physicalQuantity()).isEqualTo(12);
        assertThat(r8.reservedQuantity()).isEqualTo(4);
        assertThat(r8.availableQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Paso 4.10: Aislamiento estricto entre tiendas (Alpha vs Beta)")
    void testEquivalenceMultitenancyIsolation() {
        List<InventoryResponse> betaList = getStoreInventoryService.execute(storeBeta);
        List<InventoryResponse> betaCandidate = executeCandidateBatchService(storeBeta);

        assertThat(betaCandidate).usingRecursiveComparison().isEqualTo(betaList);
        assertThat(betaCandidate).hasSize(1);

        InventoryResponse betaItem = betaCandidate.get(0);
        assertThat(betaItem.sku()).isEqualTo("SKU-B1");
        assertThat(betaItem.physicalQuantity()).isEqualTo(50);
        assertThat(betaItem.reservedQuantity()).isEqualTo(40);
        assertThat(betaItem.availableQuantity()).isEqualTo(10);

        // Confirmar que reservas de Beta no permearon a Alpha
        List<InventoryResponse> alphaList = getStoreInventoryService.execute(storeAlpha);
        boolean anyAlphaContainsBeta = alphaList.stream().anyMatch(a -> a.sku().equals("SKU-B1"));
        assertThat(anyAlphaContainsBeta).isFalse();
    }

    // =========================================================================
    // VERIFICACIÓN DE ELIMINACIÓN DE N+1 Y MEDICIÓN (Fase 3B)
    // =========================================================================

    @Test
    @DisplayName("Fase 3B: Confirma eliminación de N+1 en GetStoreInventoryService (exactamente 2 queries)")
    void testQueryCountOptimizedOnGetStoreInventory() {
        // En storeMulti tenemos 10 variantes con seguimiento y stock físico
        hibernateStatistics.clear();
        long queriesBefore = hibernateStatistics.getPrepareStatementCount();

        List<InventoryResponse> responses = getStoreInventoryService.execute(storeMulti);

        long queriesAfter = hibernateStatistics.getPrepareStatementCount();
        long totalQueries = queriesAfter - queriesBefore;

        assertThat(responses).hasSize(10);
        // Ahora debe ser exactamente 2 consultas: 1 para findAllVariantInventoriesByStoreId y 1 para sumActiveReservedQuantitiesByStoreIdAndVariantIds!
        assertThat(totalQueries).isEqualTo(2);

        // Caso límite: tienda vacía no debe disparar la consulta de reservas
        hibernateStatistics.clear();
        long queriesEmptyBefore = hibernateStatistics.getPrepareStatementCount();
        List<InventoryResponse> emptyResponses = getStoreInventoryService.execute(storeEmpty);
        long queriesEmptyAfter = hibernateStatistics.getPrepareStatementCount();
        assertThat(emptyResponses).isEmpty();
        assertThat(queriesEmptyAfter - queriesEmptyBefore).isEqualTo(1);
    }

    @Test
    @DisplayName("Fase 3B: Confirma eliminación de N+1 en GetLowStockInventoryService (exactamente 2 queries)")
    void testQueryCountOptimizedOnGetLowStock() {
        // En storeAlpha hay 2 variantes en condición de low stock (V2 y V3)
        hibernateStatistics.clear();
        long queriesBefore = hibernateStatistics.getPrepareStatementCount();

        List<InventoryResponse> lowStock = getLowStockInventoryService.execute(storeAlpha);

        long queriesAfter = hibernateStatistics.getPrepareStatementCount();
        long totalQueries = queriesAfter - queriesBefore;

        assertThat(lowStock).hasSize(2);
        // Ahora debe ser exactamente 2 consultas: 1 para findLowStockVariantsByStoreId y 1 para sumActiveReservedQuantitiesByStoreIdAndVariantIds!
        assertThat(totalQueries).isEqualTo(2);
    }

    @Test
    @DisplayName("Fase 3B: Benchmark de rendimiento post-optimización con estadísticas (avg, median, p95)")
    void testPerformanceMeasurementBenchmark() {
        int warmup = 15;
        int iterations = 50;

        // Warmup
        for (int i = 0; i < warmup; i++) {
            getStoreInventoryService.execute(storeMulti);
        }

        double[] latenciesMs = new double[iterations];

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            getStoreInventoryService.execute(storeMulti);
            long end = System.nanoTime();
            latenciesMs[i] = (end - start) / 1_000_000.0;
        }

        Arrays.sort(latenciesMs);
        double sum = 0;
        for (double d : latenciesMs) sum += d;
        double avgMs = sum / iterations;
        double medianMs = latenciesMs[iterations / 2];
        double p95Ms = latenciesMs[(int) (iterations * 0.95)];

        System.out.println("=== BENCHMARK DE RENDIMIENTO FASE 3B (50 iteraciones en storeMulti con 10 variantes) ===");
        System.out.printf("Consultas SQL por petición: 2 (antes 11)%n");
        System.out.printf("Latencia Promedio: %.3f ms/req (antes ~10.04 ms/req)%n", avgMs);
        System.out.printf("Latencia Mediana (P50): %.3f ms/req%n", medianMs);
        System.out.printf("Latencia Percentil 95 (P95): %.3f ms/req%n", p95Ms);

        // Aserción de no regresión y latencia controlada en entorno local
        assertThat(avgMs).isLessThan(8.0);
        assertThat(p95Ms).isLessThan(15.0);
    }
}
