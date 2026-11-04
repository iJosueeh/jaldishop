package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.realtime.application.service.RealtimeNotificationPublisher;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de integración contra PostgreSQL real con proxies @Transactional y bloqueo pesimista activos.
 *
 * <p>Verifica:</p>
 * <ul>
 *   <li>Requisito 6: Serialización estricta de transacciones concurrentes mediante SELECT ... FOR UPDATE,
 *       previniendo pérdida de actualizaciones (Lost Updates) y asegurando consistencia en la máquina de estados.</li>
 *   <li>Requisito 7: Rollback transaccional: no emite eventos WebSocket si la transacción se revierte.</li>
 *   <li>Requisito 8: Commit transaccional: emite la notificación WebSocket AFTER_COMMIT con los datos confirmados.</li>
 * </ul>
 */
@SpringBootTest
@Import(ChangeOrderStatusConcurrencyIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChangeOrderStatusConcurrencyIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();
    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");

    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdUserIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
    private ChangeOrderStatusService changeOrderStatusService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RealtimeNotificationPublisher notificationPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(notificationPublisher);
    }

    @AfterAll
    static void cleanUpTestData() throws SQLException {
        if (createdOrderIds.isEmpty()) {
            return;
        }
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
    @DisplayName("Requisito 6 (Concurrencia): Dos transacciones simultáneas se serializan por PESSIMISTIC_WRITE sin pérdida de actualización")
    void shouldSerializeConcurrentStatusChangesWithoutLostUpdate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-CONC-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, userId, OrderStatus.CONFIRMED);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // Hilo 1: intenta avanzar a IN_PREPARATION
        executor.submit(() -> {
            try {
                startGate.await();
                changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_ID);
                successCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        // Hilo 2: intenta avanzar a CANCELLED
        executor.submit(() -> {
            try {
                startGate.await();
                changeOrderStatusService.execute(orderId, OrderStatus.CANCELLED, STORE_ID);
                successCount.incrementAndGet();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();

        // Verificación de serialización estricta por PESSIMISTIC_WRITE:
        // Caso A: Hilo 1 adquiere lock primero (CONFIRMED -> IN_PREPARATION). Hilo 2 adquiere lock luego (IN_PREPARATION -> CANCELLED, válida).
        //         Ambos triunfan (successCount = 2, errors = 0).
        // Caso B: Hilo 2 adquiere lock primero (CONFIRMED -> CANCELLED). Hilo 1 adquiere lock luego y lee CANCELLED;
        //         al ser terminal, Order.transitionTo(IN_PREPARATION) lanza BusinessRuleException RN-ORD-07 (successCount = 1, errors = 1 RN-ORD-07).
        // En NINGÚN caso hay pérdida de actualización (Lost Update) ni estado inconsistente.
        if (errors.isEmpty()) {
            assertThat(successCount.get()).isEqualTo(2);
        } else {
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(errors).hasSize(1);
            assertThat(errors.peek())
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Transición inválida de estado");
        }

        // Validar estado final consistente en PostgreSQL real
        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(finalOrder.getStatusHistory().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Requisito 6 (Idempotencia concurrente): Dos hilos intentando el mismo estado no duplican eventos ni fallan")
    void shouldBeIdempotentAndSerializeWhenTwoThreadsAttemptSameTransition() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-IDEM-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, userId, OrderStatus.CONFIRMED);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_ID);
                    successCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(errors).isEmpty();
        assertThat(successCount.get()).isEqualTo(2);

        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);

        // Solo 1 evento emitido vía WebSocket a pesar de haber 2 ejecuciones concurrentes
        verify(notificationPublisher, times(1)).publishOrderStatusChanged(any(OrderStatusChangedDomainEvent.class));
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Requisito 7 (Rollback): No se emite evento WebSocket si la transacción hace rollback")
    void shouldNotPublishWebSocketEventWhenTransactionRollsBack() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-ROLL-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, userId, OrderStatus.CONFIRMED);

        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() ->
            template.execute(status -> {
                changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_ID);
                // Forzar rollback explícito dentro de la misma transacción
                throw new IllegalStateException("Simulando fallo de infraestructura que provoca rollback");
            })
        ).isInstanceOf(IllegalStateException.class);

        // 1. En PostgreSQL real, el pedido continúa en CONFIRMED
        Order orderAfterRollback = orderRepository.findById(orderId).orElseThrow();
        assertThat(orderAfterRollback.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // 2. El listener AFTER_COMMIT NUNCA fue ejecutado
        verify(notificationPublisher, never()).publishOrderStatusChanged(any());
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Requisito 8 (Commit): Emite la notificación WebSocket AFTER_COMMIT únicamente con la transición confirmada")
    void shouldPublishWebSocketEventWhenTransactionCommits() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String orderNumber = "JAL-COMM-" + UUID.randomUUID().toString().substring(0, 8);
        seedOrderInPostgres(orderId, orderNumber, userId, OrderStatus.CONFIRMED);

        TransactionTemplate template = new TransactionTemplate(transactionManager);

        template.executeWithoutResult(status -> {
            changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_ID);
        });

        // 1. En PostgreSQL real, el pedido quedó actualizado a IN_PREPARATION
        Order confirmedOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(confirmedOrder.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);

        // 2. El listener AFTER_COMMIT se ejecutó y publicó exactamente el evento confirmado
        ArgumentCaptor<OrderStatusChangedDomainEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(notificationPublisher, times(1)).publishOrderStatusChanged(captor.capture());

        OrderStatusChangedDomainEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(orderId);
        assertThat(event.orderNumber()).isEqualTo(orderNumber);
        assertThat(event.customerId()).isEqualTo(userId);
        assertThat(event.storeId()).isEqualTo(STORE_ID);
        assertThat(event.previousStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(event.newStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
    }

    private void seedOrderInPostgres(UUID orderId, String orderNumber, UUID userId, OrderStatus initialStatus) {
        UUID capResId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String email = "order-user-" + UUID.randomUUID().toString().substring(0, 8) + "@jaldi.test";

        createdOrderIds.add(orderId);
        createdPaymentIds.add(paymentId);
        createdCapResIds.add(capResId);
        createdUserIds.add(userId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Insert user
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at)
                    VALUES (?, ?, '$2a$10$abcdefghijklmnopqrstuv', 'Test', 'User', 'ACTIVE', NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, userId);
                ps.setString(2, email);
                ps.executeUpdate();
            }

            // 2. Insert capacity reservation
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, payment_protection_expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, CURRENT_DATE + INTERVAL '30 days', '10:00:00', '12:00:00', 'COMMITTED', NOW() + INTERVAL '30 minutes', NULL, NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, capResId);
                ps.setObject(2, STORE_ID);
                ps.setObject(3, userId);
                ps.executeUpdate();
            }

            // 3. Insert payment
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, approved_at, created_at, updated_at)
                    VALUES (?, ?, 50.00, 'PEN', 'APPROVED', 'NOT_REQUIRED', NOW(), NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, paymentId);
                ps.setObject(2, capResId);
                ps.executeUpdate();
            }

            // 4. Insert order
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO orders (id, order_number, user_id, store_id, payment_id, capacity_reservation_id, status, delivery_mode, service_date, service_start_time, service_end_time,
                                        customer_name, customer_phone, customer_email, delivery_address, delivery_reference, currency,
                                        products_subtotal_amount, discount_amount, delivery_fee_amount, included_tax_amount, total_amount, confirmed_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'PICKUP', CURRENT_DATE + INTERVAL '30 days', '10:00:00', '12:00:00',
                            'Cliente Test', '987654321', ?, NULL, NULL, 'PEN',
                            50.00, 0.00, 0.00, 7.63, 50.00, NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, orderId);
                ps.setString(2, orderNumber);
                ps.setObject(3, userId);
                ps.setObject(4, STORE_ID);
                ps.setObject(5, paymentId);
                ps.setObject(6, capResId);
                ps.setString(7, initialStatus.name());
                ps.setString(8, email);
                ps.executeUpdate();
            }

            // 5. Insert initial status history
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO order_status_history (id, order_id, changed_by_user_id, status, reason, changed_at)
                    VALUES (?, ?, NULL, ?, 'Pedido confirmado inicialmente', NOW());
                    """)) {
                ps.setObject(1, UUID.randomUUID());
                ps.setObject(2, orderId);
                ps.setString(3, initialStatus.name());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al preparar datos de prueba en PostgreSQL: " + e.getMessage(), e);
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

        @Bean
        @Primary
        RealtimeNotificationPublisher realtimeNotificationPublisher() {
            return Mockito.mock(RealtimeNotificationPublisher.class);
        }
    }

}
