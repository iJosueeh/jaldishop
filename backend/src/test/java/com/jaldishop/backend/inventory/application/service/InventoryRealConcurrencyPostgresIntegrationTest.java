package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class InventoryRealConcurrencyPostgresIntegrationTest {

    @Autowired
    private UpdateInventoryStockService updateInventoryStockService;
    @Autowired
    private ReserveInventoryService reserveInventoryService;
    @Autowired
    private CommitInventoryReservationService commitInventoryReservationService;
    @Autowired
    private RestockInventoryService restockInventoryService;
    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID VARIANT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302"); // seeded in V4

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        dbUrl = config.url();
        dbUser = config.username();
        dbPassword = config.password();
        registry.add("spring.datasource.url", config::url);
        registry.add("spring.datasource.username", config::username);
        registry.add("spring.datasource.password", config::password);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeAll
    static void migrateDb() {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        Flyway flyway = Flyway.configure()
                .dataSource(config.url(), config.username(), config.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(config.validateOnMigrate())
                .load();
        flyway.migrate();
    }

    @Test
    @DisplayName("Concurrencia real en PostgreSQL: ajuste manual de stock no permite sobreventa ante hold activo")
    void testRealConcurrentHoldAndManualAdjustment() throws SQLException {
        UUID capacityReservationId = UUID.randomUUID();
        UUID userId = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2"); // customer seeded in V4

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // 1. Inicializar stock físico en 10
            try (var s = conn.prepareStatement("UPDATE inventories SET quantity = 10 WHERE variant_id = ?")) {
                s.setObject(1, VARIANT_ID);
                s.executeUpdate();
            }

            // 2. Crear una reserva de capacidad activa con expiración futura
            try (var s = conn.prepareStatement("""
                INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, expires_at, status, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_DATE, '10:00:00', '11:00:00', NOW() + INTERVAL '10 minutes', 'ACTIVE', NOW(), NOW())
                """)) {
                s.setObject(1, capacityReservationId);
                s.setObject(2, STORE_ID);
                s.setObject(3, userId);
                s.executeUpdate();
            }

            // 3. Crear hold de inventario para 6 unidades
            CheckoutItemSnapshot item = new CheckoutItemSnapshot(
                    VARIANT_ID,
                    UUID.randomUUID(),
                    "Café",
                    "250g",
                    "SKU",
                    null,
                    6,
                    new BigDecimal("25.00"),
                    "PEN",
                    new BigDecimal("150.00"),
                    true
            );
            reserveInventoryService.execute(STORE_ID, capacityReservationId, Instant.now().plus(10, ChronoUnit.MINUTES), List.of(item));

            // 4. Intentar reducir stock físico manualmente a 4 unidades (menor que el hold activo de 6 unidades)
            // Debe fallar con CANNOT_REDUCE_BELOW_RESERVED
            boolean conflictOccurred = false;
            try {
                updateInventoryStockService.execute(STORE_ID, VARIANT_ID, 4);
            } catch (ConflictException ex) {
                conflictOccurred = true;
                assertThat(ex.getCode()).isEqualTo("CANNOT_REDUCE_BELOW_RESERVED");
            }
            assertThat(conflictOccurred).isTrue();

            // 5. Ajustar a 8 unidades (mayor que el hold de 6): debe tener éxito
            InventoryResponse resp = updateInventoryStockService.execute(STORE_ID, VARIANT_ID, 8);
            assertThat(resp.physicalQuantity()).isEqualTo(8);
            assertThat(resp.reservedQuantity()).isEqualTo(6);
            assertThat(resp.availableQuantity()).isEqualTo(2);

            // Limpieza
            try (var s = conn.prepareStatement("DELETE FROM inventory_reservations WHERE capacity_reservation_id = ?")) {
                s.setObject(1, capacityReservationId);
                s.executeUpdate();
            }
            try (var s = conn.prepareStatement("DELETE FROM capacity_reservations WHERE id = ?")) {
                s.setObject(1, capacityReservationId);
                s.executeUpdate();
            }
        }
    }

    @Test
    @DisplayName("Concurrencia real multihilo en PostgreSQL: múltiples hilos ejecutando PUT /stock simultáneo")
    void testRealMultithreadedPutStockPostgres() throws InterruptedException, ExecutionException {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();

        List<Future<?>> futures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int targetQty = 20 + i;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    updateInventoryStockService.execute(STORE_ID, VARIANT_ID, targetQty);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        startLatch.countDown();

        for (Future<?> f : futures) {
            f.get();
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threads);

        // El stock final en DB debe ser uno de los valores válidos asignados (sin corrupción de datos ni deadlocks)
        Optional<com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection> opt =
                inventoryJpaRepository.findVariantInventoryByStoreIdAndVariantId(STORE_ID, VARIANT_ID);
        assertThat(opt).isPresent();
        assertThat(opt.get().getPhysicalQuantity()).isBetween(20, 24);
    }
}
