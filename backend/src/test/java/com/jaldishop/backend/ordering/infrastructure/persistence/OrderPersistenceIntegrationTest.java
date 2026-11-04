package com.jaldishop.backend.ordering.infrastructure.persistence;

import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración de persistencia con PostgreSQL real y Flyway schema.
 *
 * <p>Valida:</p>
 * <ul>
 *   <li>Paginación real en BD con tamaño predeterminado de 20 registros por tienda.</li>
 *   <li>Ordenación estricta descendente por fecha de confirmación (confirmed_at DESC).</li>
 *   <li>Filtro opcional por estado (OrderStatus).</li>
 *   <li>Aislamiento multitienda en consultas de listado y de detalle (por ID y por orderNumber).</li>
 * </ul>
 */
@SpringBootTest
@Import(OrderPersistenceIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderPersistenceIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    // Tiendas dedicadas para la prueba de persistencia
    private static UUID storeAId;
    private static UUID storeBId;
    private static UUID customerUserId;
    private static final List<UUID> createdUserIds = new ArrayList<>();
    private static final List<UUID> createdStoreIds = new ArrayList<>();

    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

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

    private static final List<UUID> storeAOrderIds = new ArrayList<>();
    private static final List<String> storeAOrderNumbers = new ArrayList<>();
    private static UUID storeBOrderId;
    private static String storeBOrderNumber;

    @BeforeAll
    static void seedDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Crear usuarios y tiendas dedicadas aisladas del seed demo V4
            customerUserId = createTestUser(conn, "customer.persist@" + UUID.randomUUID() + ".com");
            UUID ownerA = createTestUser(conn, "owner.persist.a@" + UUID.randomUUID() + ".com");
            UUID ownerB = createTestUser(conn, "owner.persist.b@" + UUID.randomUUID() + ".com");

            storeAId = createTestStore(conn, ownerA, "store-persist-a-" + UUID.randomUUID());
            storeBId = createTestStore(conn, ownerB, "store-persist-b-" + UUID.randomUUID());
        }

        Instant baseTime = Instant.now().minus(2, ChronoUnit.HOURS);

        // Sembrar 25 pedidos en Tienda A con confirmed_at espaciados ascendentemente
        // Estados: 15 CONFIRMED, 6 IN_PREPARATION, 4 READY
        for (int i = 0; i < 25; i++) {
            UUID orderId = UUID.randomUUID();
            String orderNumber = "JAL-PERSIST-A-" + String.format("%03d", i + 1);
            OrderStatus status;
            if (i < 15) {
                status = OrderStatus.CONFIRMED;
            } else if (i < 21) {
                status = OrderStatus.IN_PREPARATION;
            } else {
                status = OrderStatus.READY;
            }

            Instant confirmedAt = baseTime.plus(i * 2, ChronoUnit.MINUTES);
            seedOrder(orderId, orderNumber, customerUserId, storeAId, status, confirmedAt);
            storeAOrderIds.add(orderId);
            storeAOrderNumbers.add(orderNumber);
        }

        // Sembrar 5 pedidos en Tienda B
        for (int i = 0; i < 5; i++) {
            UUID orderId = UUID.randomUUID();
            String orderNumber = "JAL-PERSIST-B-" + String.format("%03d", i + 1);
            Instant confirmedAt = baseTime.plus(i * 3, ChronoUnit.MINUTES);
            seedOrder(orderId, orderNumber, customerUserId, storeBId, OrderStatus.CONFIRMED, confirmedAt);
            if (i == 0) {
                storeBOrderId = orderId;
                storeBOrderNumber = orderNumber;
            }
        }
    }

    private static UUID createTestUser(Connection conn, String email) throws SQLException {
        UUID userId = UUID.randomUUID();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'hash', 'Test', 'User', 'ACTIVE', NOW(), NOW())")) {
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
                        "VALUES (?, ?, 'Tienda Test', ?, true, false, 'ACTIVE', NOW(), NOW())")) {
            ps.setObject(1, storeId);
            ps.setObject(2, ownerUserId);
            ps.setString(3, slug);
            ps.executeUpdate();
        }
        createdStoreIds.add(storeId);
        return storeId;
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
                try (var ps = conn.prepareStatement("DELETE FROM order_status_history WHERE order_id IN (SELECT id FROM orders WHERE capacity_reservation_id = ?)")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE capacity_reservation_id = ?)")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
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
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Paginación: página 0 con size=20 retorna los 20 pedidos más recientes de la tienda ordenados DESC")
    void shouldPaginateOrdersFirstPageDescending() {
        Page<Order> page0 = orderRepository.findByStoreId(storeAId, PageRequest.of(0, 20));

        assertThat(page0).isNotNull();
        assertThat(page0.getContent()).hasSize(20);
        assertThat(page0.getTotalElements()).isEqualTo(25);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getNumber()).isZero();
        assertThat(page0.isFirst()).isTrue();
        assertThat(page0.isLast()).isFalse();

        // Verificar que todos pertenecen a la Tienda A (aislamiento multitienda)
        assertThat(page0.getContent()).allMatch(o -> o.getStoreId().equals(storeAId));

        // Verificar ordenamiento descendente estricto por confirmedAt
        List<Order> content = page0.getContent();
        for (int i = 0; i < content.size() - 1; i++) {
            Instant current = content.get(i).getConfirmedAt();
            Instant next = content.get(i + 1).getConfirmedAt();
            assertThat(current).isAfterOrEqualTo(next);
        }

        // El primer pedido de la página 0 debe ser el más recientemente sembrado (índice 24)
        assertThat(content.get(0).getOrderNumber()).isEqualTo("JAL-PERSIST-A-025");
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Paginación: página 1 con size=20 retorna los 5 pedidos restantes de la tienda")
    void shouldPaginateOrdersSecondPage() {
        Page<Order> page1 = orderRepository.findByStoreId(storeAId, PageRequest.of(1, 20));

        assertThat(page1).isNotNull();
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isEqualTo(25);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getNumber()).isEqualTo(1);
        assertThat(page1.isFirst()).isFalse();
        assertThat(page1.isLast()).isTrue();

        // El último pedido de la página 1 debe ser el más antiguo sembrado (índice 0)
        assertThat(page1.getContent().get(4).getOrderNumber()).isEqualTo("JAL-PERSIST-A-001");
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Filtro por estado: retorna únicamente los pedidos en el estado solicitado para la tienda")
    void shouldFilterOrdersByStatusWithPagination() {
        // Consultar estado IN_PREPARATION (se sembraron 6)
        Page<Order> inPrepPage = orderRepository.findByStoreIdAndStatus(
                storeAId, OrderStatus.IN_PREPARATION, PageRequest.of(0, 20)
        );

        assertThat(inPrepPage).isNotNull();
        assertThat(inPrepPage.getTotalElements()).isEqualTo(6);
        assertThat(inPrepPage.getContent()).hasSize(6);
        assertThat(inPrepPage.getContent()).allMatch(o -> o.getStatus() == OrderStatus.IN_PREPARATION);
        assertThat(inPrepPage.getContent()).allMatch(o -> o.getStoreId().equals(storeAId));

        // Consultar estado READY (se sembraron 4)
        Page<Order> readyPage = orderRepository.findByStoreIdAndStatus(
                storeAId, OrderStatus.READY, PageRequest.of(0, 20)
        );

        assertThat(readyPage).isNotNull();
        assertThat(readyPage.getTotalElements()).isEqualTo(4);
        assertThat(readyPage.getContent()).hasSize(4);
        assertThat(readyPage.getContent()).allMatch(o -> o.getStatus() == OrderStatus.READY);

        // Consultar estado CANCELLED (se sembraron 0)
        Page<Order> cancelledPage = orderRepository.findByStoreIdAndStatus(
                storeAId, OrderStatus.CANCELLED, PageRequest.of(0, 20)
        );

        assertThat(cancelledPage).isNotNull();
        assertThat(cancelledPage.getTotalElements()).isZero();
        assertThat(cancelledPage.getContent()).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Aislamiento Multitienda: los pedidos de Tienda B no aparecen en consultas de Tienda A")
    void shouldIsolateOrdersBetweenStoresInPagination() {
        Page<Order> storeBPage = orderRepository.findByStoreId(storeBId, PageRequest.of(0, 20));

        assertThat(storeBPage.getTotalElements()).isEqualTo(5);
        assertThat(storeBPage.getContent()).allMatch(o -> o.getStoreId().equals(storeBId));
        assertThat(storeBPage.getContent()).noneMatch(o -> o.getStoreId().equals(storeAId));
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Detalle por ID y Tienda: retorna el pedido solo si pertenece a la tienda indicada")
    void shouldFindByIdAndStoreIdWithStrictOwnership() {
        UUID orderAId = storeAOrderIds.get(0);

        // 1. Con la tienda correcta: lo encuentra
        Optional<Order> found = orderRepository.findByIdAndStoreId(orderAId, storeAId);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(orderAId);
        assertThat(found.get().getStoreId()).isEqualTo(storeAId);

        // 2. Con una tienda ajena (Tienda B): no lo encuentra (Optional.empty)
        Optional<Order> crossTenant = orderRepository.findByIdAndStoreId(orderAId, storeBId);
        assertThat(crossTenant).isEmpty();

        // 3. Con un UUID inexistente: no lo encuentra
        Optional<Order> nonExistent = orderRepository.findByIdAndStoreId(UUID.randomUUID(), storeAId);
        assertThat(nonExistent).isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Detalle por orderNumber y Tienda: retorna el pedido solo si coincide con la tienda")
    void shouldFindByOrderNumberAndStoreIdWithStrictOwnership() {
        String orderANumber = storeAOrderNumbers.get(0);

        // 1. Con la tienda correcta: lo encuentra
        Optional<Order> found = orderRepository.findByOrderNumberAndStoreId(orderANumber, storeAId);
        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo(orderANumber);
        assertThat(found.get().getStoreId()).isEqualTo(storeAId);

        // 2. Con una tienda ajena (Tienda B): no lo encuentra
        Optional<Order> crossTenant = orderRepository.findByOrderNumberAndStoreId(orderANumber, storeBId);
        assertThat(crossTenant).isEmpty();

        // 3. Pedido de Tienda B con Tienda B: lo encuentra
        Optional<Order> foundB = orderRepository.findByOrderNumberAndStoreId(storeBOrderNumber, storeBId);
        assertThat(foundB).isPresent();
        assertThat(foundB.get().getOrderNumber()).isEqualTo(storeBOrderNumber);
        assertThat(foundB.get().getStoreId()).isEqualTo(storeBId);

        // 4. Número inexistente: no lo encuentra
        Optional<Order> nonExistent = orderRepository.findByOrderNumberAndStoreId("JAL-NONEXISTENT", storeAId);
        assertThat(nonExistent).isEmpty();
    }

    private static void seedOrder(UUID orderId, String orderNumber, UUID userId, UUID storeId,
                                  OrderStatus status, Instant confirmedAt) throws SQLException {
        UUID capResId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        createdOrderIds.add(orderId);
        createdPaymentIds.add(paymentId);
        createdCapResIds.add(capResId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Capacity reservation
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, CURRENT_DATE + INTERVAL '10 days', '10:00:00', '12:00:00', 'COMMITTED', NOW() + INTERVAL '30 minutes', NULL, ?, ?)
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, capResId);
                ps.setObject(2, storeId);
                ps.setObject(3, userId);
                ps.setObject(4, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(5, java.sql.Timestamp.from(confirmedAt));
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
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'PICKUP', CURRENT_DATE + INTERVAL '10 days', '10:00:00', '12:00:00',
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
                ps.setObject(8, java.sql.Timestamp.from(confirmedAt));
                ps.setObject(9, java.sql.Timestamp.from(confirmedAt));
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

}
