package com.jaldishop.backend.capacity.infrastructure.persistence;

import com.jaldishop.backend.capacity.application.occupancy.service.GetMerchantStoreCapacityOccupancyService;
import com.jaldishop.backend.capacity.web.dto.MerchantStoreCapacityOccupancyResponse;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(MerchantCapacityPostgresIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MerchantCapacityPostgresIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class SafeTestDatabase {
        @Bean
        DataSource dataSource() {
            TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(config.url());
            dataSource.setUsername(config.username());
            dataSource.setPassword(config.password());
            return dataSource;
        }
    }

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    private static UUID storeAId;
    private static UUID storeBId;
    private static UUID customerUserId;

    private static final List<UUID> createdUserIds = new ArrayList<>();
    private static final List<UUID> createdStoreIds = new ArrayList<>();
    private static final Set<UUID> createdConfigIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdExceptionIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdReservationIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Fecha fija de prueba unificada (un lunes: dayOfWeek = 1)
    private static final LocalDate TEST_DATE = LocalDate.of(2027, 5, 10);
    private static final LocalDate OTHER_DATE = LocalDate.of(2027, 5, 11);

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
    private GetMerchantStoreCapacityOccupancyService occupancyService;

    @BeforeAll
    static void seedDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            customerUserId = createTestUser(conn, "customer.cap." + UUID.randomUUID() + "@jaldishop.test");
            UUID ownerA = createTestUser(conn, "owner.cap.a." + UUID.randomUUID() + "@jaldishop.test");
            UUID ownerB = createTestUser(conn, "owner.cap.b." + UUID.randomUUID() + "@jaldishop.test");

            storeAId = createTestStore(conn, ownerA, "store-cap-a-" + UUID.randomUUID());
            storeBId = createTestStore(conn, ownerB, "store-cap-b-" + UUID.randomUUID());

            // Tienda A: Configuraciones base de lunes (day_of_week = 1)
            // Franja 1: 09:00 - 13:00 (capacidad 10)
            createCapacityConfiguration(conn, storeAId, 1, LocalTime.of(9, 0), LocalTime.of(13, 0), 10, "ACTIVE");
            // Franja 2: 14:00 - 18:00 (capacidad 15)
            createCapacityConfiguration(conn, storeAId, 1, LocalTime.of(14, 0), LocalTime.of(18, 0), 15, "ACTIVE");

            // Sembrar reservas para Tienda A en TEST_DATE:
            Instant now = Instant.now();
            Instant futureTtl = now.plus(10, ChronoUnit.MINUTES);
            Instant pastTtl = now.minus(5, ChronoUnit.MINUTES);

            // Franja 1 (09:00 - 13:00):
            // - 1 ACTIVE no vencida (cuenta)
            // - 1 ACTIVE vencida (NO cuenta)
            // - 1 PAYMENT_PROTECTED no vencida (cuenta)
            // - 1 PAYMENT_PROTECTED vencida (NO cuenta)
            // - 2 COMMITTED (cuenta)
            // - 1 RELEASED (NO cuenta)
            // - 1 EXPIRED (NO cuenta)
            // Total ocupado esperado franja 1 = 1 + 1 + 2 = 4 cupos.
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "ACTIVE", futureTtl, null);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "ACTIVE", pastTtl, null);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "PAYMENT_PROTECTED", pastTtl, futureTtl);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "PAYMENT_PROTECTED", pastTtl, pastTtl);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "RELEASED", pastTtl, null);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "EXPIRED", pastTtl, null);

            // Franja 2 (14:00 - 18:00):
            // - 3 COMMITTED
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(14, 0), LocalTime.of(18, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(14, 0), LocalTime.of(18, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeAId, customerUserId, TEST_DATE, LocalTime.of(14, 0), LocalTime.of(18, 0),
                    "COMMITTED", pastTtl, null);

            // Tienda B (Aislamiento multitienda):
            // - 5 COMMITTED en 09:00 - 13:00 en TEST_DATE
            createReservation(conn, storeBId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeBId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeBId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeBId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "COMMITTED", pastTtl, null);
            createReservation(conn, storeBId, customerUserId, TEST_DATE, LocalTime.of(9, 0), LocalTime.of(13, 0),
                    "COMMITTED", pastTtl, null);
        }
    }

    @AfterAll
    static void cleanupDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            for (UUID resId : createdReservationIds) {
                deleteById(conn, "capacity_reservations", resId);
            }
            for (UUID excId : createdExceptionIds) {
                deleteById(conn, "capacity_exceptions", excId);
            }
            for (UUID cfgId : createdConfigIds) {
                deleteById(conn, "capacity_configurations", cfgId);
            }
            for (UUID storeId : createdStoreIds) {
                deleteById(conn, "stores", storeId);
            }
            for (UUID userId : createdUserIds) {
                deleteById(conn, "users", userId);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("C1: Ocupación real estándar con franjas disjuntas y vigencia de reservas verificada")
    void shouldCalculateRealOccupancyAccurately() {
        MerchantStoreCapacityOccupancyResponse response = occupancyService.execute(storeAId, TEST_DATE);

        assertThat(response.storeId()).isEqualTo(storeAId);
        assertThat(response.date()).isEqualTo(TEST_DATE);
        assertThat(response.isClosed()).isFalse();

        // Franja 1 (09-13, cap 10) ocupada con 4 (1 ACTIVE vivo + 1 PAYMENT_PROTECTED vivo + 2 COMMITTED)
        // Franja 2 (14-18, cap 15) ocupada con 3 COMMITTED
        // Total efectivo = 10 + 15 = 25
        assertThat(response.effectiveCapacity()).isEqualTo(25);
        // Total ocupado = 4 + 3 = 7
        assertThat(response.occupiedCapacity()).isEqualTo(7);
        assertThat(response.availableCapacity()).isEqualTo(18);
        assertThat(response.reservedCapacity()).isEqualTo(2); // 1 active + 1 protected
        assertThat(response.committedCapacity()).isEqualTo(5); // 2 + 3
        assertThat(response.occupancyPercentage()).isEqualTo(28); // 7 / 25 * 100 = 28%

        assertThat(response.slots()).hasSize(2);
        assertThat(response.slots().get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.slots().get(0).effectiveCapacity()).isEqualTo(10);
        assertThat(response.slots().get(0).occupiedCapacity()).isEqualTo(4);
        assertThat(response.slots().get(0).availableCapacity()).isEqualTo(6);

        assertThat(response.slots().get(1).startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.slots().get(1).effectiveCapacity()).isEqualTo(15);
        assertThat(response.slots().get(1).occupiedCapacity()).isEqualTo(3);
        assertThat(response.slots().get(1).availableCapacity()).isEqualTo(12);
    }

    @Test
    @Order(2)
    @DisplayName("Aislamiento multitienda: Tienda B no contamina ni ve reservas de Tienda A")
    void shouldEnforceMultitenantIsolation() {
        MerchantStoreCapacityOccupancyResponse responseB = occupancyService.execute(storeBId, TEST_DATE);

        assertThat(responseB.storeId()).isEqualTo(storeBId);
        // Tienda B no tiene configuración, por lo que effectiveCapacity = 0
        // Pero tiene 5 reservas COMMITTED que deben seguir visibles
        assertThat(responseB.occupiedCapacity()).isEqualTo(5);
        assertThat(responseB.committedCapacity()).isEqualTo(5);
        assertThat(responseB.reservedCapacity()).isEqualTo(0);
        assertThat(responseB.availableCapacity()).isEqualTo(0);
        assertThat(responseB.isClosed()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("C7: Fecha sin reservas ni excepciones ni configuración retorna ceros")
    void shouldReturnZerosForEmptyDate() {
        MerchantStoreCapacityOccupancyResponse response = occupancyService.execute(storeBId, OTHER_DATE);

        assertThat(response.effectiveCapacity()).isZero();
        assertThat(response.occupiedCapacity()).isZero();
        assertThat(response.availableCapacity()).isZero();
        assertThat(response.slots()).isEmpty();
        assertThat(response.isClosed()).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("C2: Excepción reduce capacidad bajo reservas existentes (sobrecapacidad > 100%)")
    void shouldHandleOvercapacityWhenExceptionReducesCapacity() throws SQLException {
        LocalDate dateC2 = LocalDate.of(2027, 5, 17); // Siguiente lunes
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            // Excepción en 09:00 - 13:00 con capacidad 2
            createCapacityException(conn, storeAId, dateC2, LocalTime.of(9, 0), LocalTime.of(13, 0), 2, "ACTIVE");
            // 4 reservas COMMITTED
            for (int i = 0; i < 4; i++) {
                createReservation(conn, storeAId, customerUserId, dateC2, LocalTime.of(9, 0), LocalTime.of(13, 0),
                        "COMMITTED", Instant.now().minus(1, ChronoUnit.HOURS), null);
            }
        }

        MerchantStoreCapacityOccupancyResponse response = occupancyService.execute(storeAId, dateC2);

        // Franja 09-13 tiene cap 2 y ocupado 4 -> disponible 0, porcentaje 200%
        // Franja 14-18 tiene cap base 15 y ocupado 0 -> disponible 15
        assertThat(response.slots().get(0).effectiveCapacity()).isEqualTo(2);
        assertThat(response.slots().get(0).occupiedCapacity()).isEqualTo(4);
        assertThat(response.slots().get(0).availableCapacity()).isEqualTo(0);
        assertThat(response.slots().get(0).occupancyPercentage()).isEqualTo(200);

        assertThat(response.effectiveCapacity()).isEqualTo(17); // 2 + 15
        assertThat(response.occupiedCapacity()).isEqualTo(4);
        assertThat(response.availableCapacity()).isEqualTo(13);
    }

    @Test
    @Order(5)
    @DisplayName("C3: Excepción de jornada completa cierra la tienda pero mantiene reservas visibles")
    void shouldHandleFullDayExceptionClosureWithCommittedReservations() throws SQLException {
        LocalDate dateC3 = LocalDate.of(2027, 5, 24); // Lunes
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            // Excepción de jornada completa cerrada (capacidad 0)
            createCapacityException(conn, storeAId, dateC3, null, null, 0, "ACTIVE");
            // 3 reservas COMMITTED en 09-13
            for (int i = 0; i < 3; i++) {
                createReservation(conn, storeAId, customerUserId, dateC3, LocalTime.of(9, 0), LocalTime.of(13, 0),
                        "COMMITTED", Instant.now().minus(1, ChronoUnit.HOURS), null);
            }
        }

        MerchantStoreCapacityOccupancyResponse response = occupancyService.execute(storeAId, dateC3);

        assertThat(response.isClosed()).isTrue();
        assertThat(response.effectiveCapacity()).isEqualTo(0);
        assertThat(response.occupiedCapacity()).isEqualTo(3);
        assertThat(response.availableCapacity()).isEqualTo(0);
        assertThat(response.occupancyPercentage()).isEqualTo(0);
        assertThat(response.slots().get(0).effectiveCapacity()).isEqualTo(0);
        assertThat(response.slots().get(0).occupiedCapacity()).isEqualTo(3);
    }

    @Test
    @Order(6)
    @DisplayName("C6: Solapamiento entre configuración y excepción (ambas ventanas se conservan)")
    void shouldHandleOverlappingConfigAndExceptionCrossTable() throws SQLException {
        LocalDate dateC6 = LocalDate.of(2027, 5, 31); // Lunes
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            // Excepción solapada: 11:00 - 15:00 con capacidad 8
            createCapacityException(conn, storeAId, dateC6, LocalTime.of(11, 0), LocalTime.of(15, 0), 8, "ACTIVE");
            // 2 reservas en 11:00 - 15:00
            for (int i = 0; i < 2; i++) {
                createReservation(conn, storeAId, customerUserId, dateC6, LocalTime.of(11, 0), LocalTime.of(15, 0),
                        "COMMITTED", Instant.now().minus(1, ChronoUnit.HOURS), null);
            }
        }

        MerchantStoreCapacityOccupancyResponse response = occupancyService.execute(storeAId, dateC6);

        // Debe conservar 09:00 - 13:00, 11:00 - 15:00, y 14:00 - 18:00
        assertThat(response.slots()).hasSize(3);
        assertThat(response.slots().get(0).startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.slots().get(1).startTime()).isEqualTo(LocalTime.of(11, 0));
        assertThat(response.slots().get(2).startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.slots().get(1).occupiedCapacity()).isEqualTo(2);
        assertThat(response.slots().get(1).effectiveCapacity()).isEqualTo(8);
    }

    private static void createCapacityException(Connection conn, UUID storeId, LocalDate serviceDate,
                                                LocalTime startTime, LocalTime endTime,
                                                int exceptionCapacity, String status) throws SQLException {
        UUID excId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO capacity_exceptions (id, store_id, service_date, start_time, end_time, exception_capacity, reason, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'Motivo test', ?, NOW(), NOW())")) {
            ps.setObject(1, excId);
            ps.setObject(2, storeId);
            ps.setDate(3, java.sql.Date.valueOf(serviceDate));
            ps.setTime(4, startTime != null ? Time.valueOf(startTime) : null);
            ps.setTime(5, endTime != null ? Time.valueOf(endTime) : null);
            ps.setInt(6, exceptionCapacity);
            ps.setString(7, status);
            ps.executeUpdate();
        }
        createdExceptionIds.add(excId);
    }

    // =========================================================================
    // Métodos auxiliares JDBC
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
                        "VALUES (?, ?, 'Tienda Cap Test', ?, true, false, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, storeId);
            ps.setObject(2, ownerUserId);
            ps.setString(3, slug);
            ps.executeUpdate();
        }
        createdStoreIds.add(storeId);
        return storeId;
    }

    private static void createCapacityConfiguration(Connection conn, UUID storeId, int dayOfWeek,
                                                    LocalTime startTime, LocalTime endTime,
                                                    int maxCapacity, String status) throws SQLException {
        UUID configId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO capacity_configurations (id, store_id, day_of_week, start_time, end_time, max_capacity, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
            ps.setObject(1, configId);
            ps.setObject(2, storeId);
            ps.setInt(3, dayOfWeek);
            ps.setTime(4, startTime != null ? Time.valueOf(startTime) : null);
            ps.setTime(5, endTime != null ? Time.valueOf(endTime) : null);
            ps.setInt(6, maxCapacity);
            ps.setString(7, status);
            ps.executeUpdate();
        }
        createdConfigIds.add(configId);
    }

    private static void createReservation(Connection conn, UUID storeId, UUID userId, LocalDate serviceDate,
                                          LocalTime startTime, LocalTime endTime, String status,
                                          Instant expiresAt, Instant paymentProtectionExpiresAt) throws SQLException {
        UUID resId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())")) {
            ps.setObject(1, resId);
            ps.setObject(2, storeId);
            ps.setObject(3, userId);
            ps.setDate(4, java.sql.Date.valueOf(serviceDate));
            ps.setTime(5, Time.valueOf(startTime));
            ps.setTime(6, Time.valueOf(endTime));
            ps.setString(7, status);
            ps.setTimestamp(8, expiresAt != null ? Timestamp.from(expiresAt) : null);
            ps.setTimestamp(9, paymentProtectionExpiresAt != null ? Timestamp.from(paymentProtectionExpiresAt) : null);
            ps.executeUpdate();
        }
        createdReservationIds.add(resId);
    }

    private static void deleteById(Connection conn, String table, UUID id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table + " WHERE id = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }
}
