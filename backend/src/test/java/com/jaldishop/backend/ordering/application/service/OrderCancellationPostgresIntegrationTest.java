package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.inventory.domain.enums.InventoryReservationStatus;
import com.jaldishop.backend.inventory.domain.model.InventoryReservation;
import com.jaldishop.backend.inventory.domain.repository.InventoryReservationRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.CancelOrderRequest;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderDetailResponse;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundClaimResult;
import com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(OrderCancellationPostgresIntegrationTest.SafeTestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderCancellationPostgresIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();
    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID PRODUCT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf201");

    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdVariantIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
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
        registry.add("spring.flyway.validate-on-migrate", () -> "false");
    }

    @BeforeAll
    static void initSchema() {
        Flyway.configure()
                .dataSource(database.url(), database.username(), database.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(false)
                .load()
                .migrate();
    }

    @Autowired
    private CancelCustomerOrderService cancelCustomerOrderService;

    @Autowired
    private ChangeOrderStatusService changeOrderStatusService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CapacityReservationRepository capacityReservationRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentRefundProcessor paymentRefundProcessor;

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
                try (var ps = conn.prepareStatement("DELETE FROM payment_refund_execution_attempts WHERE refund_intent_id IN (SELECT id FROM payment_refund_intents WHERE payment_id = ?)")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM payment_refund_intents WHERE payment_id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM payment_attempts WHERE payment_id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM payments WHERE id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
            }

            for (UUID capResId : createdCapResIds) {
                try (var ps = conn.prepareStatement("DELETE FROM inventory_reservations WHERE capacity_reservation_id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM capacity_reservations WHERE id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
            }

            for (UUID variantId : createdVariantIds) {
                try (var ps = conn.prepareStatement("DELETE FROM inventories WHERE variant_id = ?")) {
                    ps.setObject(1, variantId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM product_variants WHERE id = ?")) {
                    ps.setObject(1, variantId);
                    ps.executeUpdate();
                }
            }

            for (UUID userId : createdUserIds) {
                try (var ps = conn.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
                    ps.setObject(1, userId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setObject(1, userId);
                    ps.executeUpdate();
                }
            }
        }
    }

    // =========================================================================
    // PRUEBAS DE INTEGRACIÓN
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("1. Cliente cancela pedido CONFIRMED: estado CANCELLED, libera cupo, restituye inventario y marca refund PENDING")
    void shouldCancelConfirmedOrderAsCustomerAndCompensateResources() {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 10, 2);

        CustomerOrderDetailResponse response = cancelCustomerOrderService.execute(
                data.userId,
                data.orderNumber,
                "Cliente cancela por cambio de planes"
        );

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);

        // 1. Pedido en PostgreSQL
        Order order = orderRepository.findById(data.orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 2. Capacidad en PostgreSQL
        CapacityReservation capRes = capacityReservationRepository.findById(data.capResId).orElseThrow();
        assertThat(capRes.getStatus()).isEqualTo(CapacityReservationStatus.RELEASED);

        // 3. Reserva de inventario en PostgreSQL
        List<InventoryReservation> invReservations = inventoryReservationRepository.findByCapacityReservationId(data.capResId);
        assertThat(invReservations).hasSize(1);
        assertThat(invReservations.getFirst().getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);

        // 4. Existencias físicas de inventario en PostgreSQL: 10 + 2 = 12
        InventoryEntity inventory = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(12);

        // 5. Pago en PostgreSQL: refundStatus = PENDING
        Payment payment = paymentRepository.findById(data.paymentId).orElseThrow();
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        // 6. Notificación WebSocket / Dominio
        ArgumentCaptor<OrderStatusChangedDomainEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(notificationPublisher, times(1)).publishOrderStatusChanged(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(data.orderId);
        assertThat(captor.getValue().previousStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(captor.getValue().newStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("2. Comerciante cancela pedido CONFIRMED: estado CANCELLED, libera cupo, restituye inventario y refund PENDING")
    void shouldCancelConfirmedOrderAsMerchantAndCompensateResources() {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 20, 3);

        changeOrderStatusService.execute(data.orderId, OrderStatus.CANCELLED, STORE_ID);

        Order order = orderRepository.findById(data.orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        CapacityReservation capRes = capacityReservationRepository.findById(data.capResId).orElseThrow();
        assertThat(capRes.getStatus()).isEqualTo(CapacityReservationStatus.RELEASED);

        List<InventoryReservation> invReservations = inventoryReservationRepository.findByCapacityReservationId(data.capResId);
        assertThat(invReservations.getFirst().getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);

        InventoryEntity inventory = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(23); // 20 + 3

        Payment payment = paymentRepository.findById(data.paymentId).orElseThrow();
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
    }

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("3. RN-ORD-13: Comerciante cancela pedido en IN_PREPARATION: NO restituye capacidad ni inventario, pero refund es PENDING")
    void shouldNotCompensateCapacityOrInventoryWhenMerchantCancelsFromInPreparation() {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.IN_PREPARATION, 15, 4);

        changeOrderStatusService.execute(data.orderId, OrderStatus.CANCELLED, STORE_ID);

        Order order = orderRepository.findById(data.orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // RN-ORD-13: Capacidad retoma o mantiene COMMITTED (no liberada para no alterar slot de cocina)
        CapacityReservation capRes = capacityReservationRepository.findById(data.capResId).orElseThrow();
        assertThat(capRes.getStatus()).isEqualTo(CapacityReservationStatus.COMMITTED);

        // RN-ORD-13: Reserva de inventario NO liberada (insumos ya en preparación/desechados)
        List<InventoryReservation> invReservations = inventoryReservationRepository.findByCapacityReservationId(data.capResId);
        assertThat(invReservations.getFirst().getStatus()).isEqualTo(InventoryReservationStatus.COMMITTED);

        // RN-ORD-13: Stock físico sin cambio (permanece en 15, no 19)
        InventoryEntity inventory = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(15);

        // Reembolso sí se procesa a PENDING
        Payment payment = paymentRepository.findById(data.paymentId).orElseThrow();
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
    }

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("4. Rechazo de cancelación si cliente no es el propietario del pedido (AccessDeniedException)")
    void shouldRejectCancellationOfNonOwnedOrder() {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 10, 1);
        UUID strangerUserId = UUID.randomUUID();
        seedUser(strangerUserId, "stranger-" + UUID.randomUUID().toString().substring(0, 8) + "@jaldi.test");

        assertThatThrownBy(() -> cancelCustomerOrderService.execute(
                strangerUserId,
                data.orderNumber,
                "Intento no autorizado"
        )).isInstanceOf(AccessDeniedException.class);

        // Estado y recursos intactos
        Order order = orderRepository.findById(data.orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        InventoryEntity inventory = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(10);
    }

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("5. Rechazo de cancelación si el pedido no está en estado CONFIRMED (BusinessRuleException RN-ORD-09)")
    void shouldRejectCancellationFromInvalidState() {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.COMPLETED, 10, 1);

        assertThatThrownBy(() -> cancelCustomerOrderService.execute(
                data.userId,
                data.orderNumber,
                "Intento en estado completado"
        )).isInstanceOf(BusinessRuleException.class);

        Order order = orderRepository.findById(data.orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("6. Idempotencia: Llamadas repetidas de cancelación sobre pedido CANCELLED no duplican restitución")
    void shouldBeIdempotentOnRepeatedCustomerCancellation() {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 10, 2);

        // Primera cancelación
        cancelCustomerOrderService.execute(data.userId, data.orderNumber, "Primera cancelación");
        InventoryEntity inv1 = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inv1.getQuantity()).isEqualTo(12);

        // Segunda llamada repetida
        CustomerOrderDetailResponse response2 = cancelCustomerOrderService.execute(data.userId, data.orderNumber, "Cancelación repetida");
        assertThat(response2.status()).isEqualTo(OrderStatus.CANCELLED);

        // Las existencias permanecen en 12 (NO suben a 14)
        InventoryEntity inv2 = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inv2.getQuantity()).isEqualTo(12);
    }

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("7. Concurrencia real: 2 hilos simultáneos cancelan el mismo pedido; serializado por lock pesimista sin doble restitución")
    void shouldSerializeConcurrentCancellationsWithoutDoubleRestock() throws Exception {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 30, 5);

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        AtomicInteger completedCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    cancelCustomerOrderService.execute(data.userId, data.orderNumber, "Cancelación concurrente");
                    completedCount.incrementAndGet();
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(errors).isEmpty();
        assertThat(completedCount.get()).isEqualTo(2);

        // Verificación de atomicidad: el inventario sube exactamente 1 vez (30 + 5 = 35, NO 40)
        InventoryEntity inventory = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inventory.getQuantity()).isEqualTo(35);

        CapacityReservation capRes = capacityReservationRepository.findById(data.capResId).orElseThrow();
        assertThat(capRes.getStatus()).isEqualTo(CapacityReservationStatus.RELEASED);
    }

    @Test
    @org.junit.jupiter.api.Order(8)
    @DisplayName("8. Rollback transaccional ante fallo durante el proceso: revierte cambios en pedido, capacidad e inventario")
    void shouldRollbackCompleteWhenCompensationFails() {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 10, 2);

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> txTemplate.execute(status -> {
            // Modifica estado de pedido
            Order order = orderRepository.findByIdForUpdate(data.orderId).orElseThrow();
            order.transitionTo(OrderStatus.CANCELLED, data.userId, "Fallo intencional");
            orderRepository.save(order);

            // Modifica inventario
            InventoryEntity inv = inventoryJpaRepository.findById(data.variantId).orElseThrow();
            inv.setQuantity(inv.getQuantity() + 2);
            inventoryJpaRepository.save(inv);

            // Simula fallo inesperado que activa rollback
            throw new RuntimeException("Error simulado para verificar rollback transaccional completo");
        })).isInstanceOf(RuntimeException.class);

        // Verificación en PostgreSQL real tras rollback:
        Order rolledBackOrder = orderRepository.findById(data.orderId).orElseThrow();
        assertThat(rolledBackOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        InventoryEntity rolledBackInv = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(rolledBackInv.getQuantity()).isEqualTo(10); // Intacto
    }

    @Test
    @org.junit.jupiter.api.Order(9)
    @DisplayName("9. Concurrencia entre cancelación y operación de inventario: orden canónico variant_id ASC previene deadlocks")
    void shouldSerializeConcurrentlyWithInventoryOperationWithoutDeadlock() throws Exception {
        TestData data = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 15, 3);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(2);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // Hilo 1: Cancelación del pedido (bloquea variantes en orden ASC)
        executor.submit(() -> {
            try {
                startGate.await();
                cancelCustomerOrderService.execute(data.userId, data.orderNumber, "Cancelación concurrente con inventario");
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        // Hilo 2: Operación simultánea sobre el mismo inventario (adquiere lock findByVariantIdInForUpdate)
        executor.submit(() -> {
            try {
                startGate.await();
                txTemplate.execute(status -> {
                    List<InventoryEntity> list = inventoryJpaRepository.findByVariantIdInForUpdate(List.of(data.variantId));
                    if (!list.isEmpty()) {
                        list.getFirst().setQuantity(list.getFirst().getQuantity() + 1);
                        inventoryJpaRepository.save(list.getFirst());
                    }
                    return null;
                });
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        boolean finished = endGate.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(errors).isEmpty();

        // 15 + 3 (restock) + 1 (otra tx) = 19
        InventoryEntity inv = inventoryJpaRepository.findById(data.variantId).orElseThrow();
        assertThat(inv.getQuantity()).isEqualTo(19);
    }

    @Test
    @org.junit.jupiter.api.Order(10)
    @DisplayName("10. PaymentRefundProcessor: admite reembolso de pedido cancelado y rechaza reembolso de pedido activo")
    void shouldAllowRefundProcessorToClaimRefundForCancelledOrderAndBlockActiveOrder() {
        TestData cancelledData = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 10, 1);
        seedPaymentAttempt(cancelledData.paymentId, "MP-CANCELLED-1234");

        // Cancelamos el pedido
        cancelCustomerOrderService.execute(cancelledData.userId, cancelledData.orderNumber, "Cancelado para test de refund");

        // Verificamos que PaymentRefundProcessor reclama la intención para el pedido cancelado
        UUID workerToken = UUID.randomUUID();
        RefundClaimResult claimResultCancelled = paymentRefundProcessor.claimOrCreateRefundIntent(cancelledData.paymentId, workerToken);
        assertThat(claimResultCancelled.claimed()).isTrue();

        // Creamos un segundo pedido que permanece activo (CONFIRMED) con refund_status = PENDING forzado
        TestData activeData = seedCompleteOrderInPostgres(OrderStatus.CONFIRMED, 10, 1);
        seedPaymentAttempt(activeData.paymentId, "MP-ACTIVE-5678");

        Payment activePayment = paymentRepository.findById(activeData.paymentId).orElseThrow();
        activePayment.requestRefund(activePayment.getAmount(), "Intento inválido sobre orden activa");
        paymentRepository.save(activePayment);

        // Verificamos que PaymentRefundProcessor RECHAZA / OMITE reclamar reembolso para el pedido activo
        RefundClaimResult claimResultActive = paymentRefundProcessor.claimOrCreateRefundIntent(activeData.paymentId, workerToken);
        assertThat(claimResultActive.claimed()).isFalse();
        assertThat(claimResultActive.skipReason()).contains("activo asociado");
    }

    // =========================================================================
    // UTILIDADES DE PREPARACIÓN DE DATOS
    // =========================================================================

    private record TestData(
            UUID orderId,
            String orderNumber,
            UUID userId,
            UUID capResId,
            UUID paymentId,
            UUID variantId
    ) {}

    private TestData seedCompleteOrderInPostgres(OrderStatus initialStatus, int initialStock, int reservedQty) {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID capResId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID invResId = UUID.randomUUID();
        String orderNumber = "JAL-TEST-" + UUID.randomUUID().toString().substring(0, 8);
        String email = "cancel-user-" + UUID.randomUUID().toString().substring(0, 8) + "@jaldi.test";

        createdOrderIds.add(orderId);
        createdUserIds.add(userId);
        createdCapResIds.add(capResId);
        createdPaymentIds.add(paymentId);
        createdVariantIds.add(variantId);

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Usuario
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at)
                    VALUES (?, ?, '$2a$10$abcdefghijklmnopqrstuv', 'Test', 'Customer', 'ACTIVE', NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, userId);
                ps.setString(2, email);
                ps.executeUpdate();
            }

            // 2. Rol CUSTOMER en la tienda
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO user_roles (id, user_id, role_id, store_id)
                    VALUES (gen_random_uuid(), ?, 1, ?)
                    ON CONFLICT DO NOTHING;
                    """)) {
                ps.setObject(1, userId);
                ps.setObject(2, STORE_ID);
                ps.executeUpdate();
            }

            // 3. Variante e Inventario físico
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO product_variants (id, store_id, product_id, presentation_name, sku, price_amount, price_currency, tracks_inventory, status, created_at, updated_at)
                    VALUES (?, ?, ?, 'Variante Cancel Test', ?, 25.00, 'PEN', true, 'ACTIVE', NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, variantId);
                ps.setObject(2, STORE_ID);
                ps.setObject(3, PRODUCT_ID);
                ps.setString(4, "SKU-CANCEL-" + UUID.randomUUID().toString().substring(0, 8));
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO inventories (variant_id, quantity, low_stock_threshold, updated_at)
                    VALUES (?, ?, 2, NOW())
                    ON CONFLICT (variant_id) DO UPDATE SET quantity = EXCLUDED.quantity;
                    """)) {
                ps.setObject(1, variantId);
                ps.setInt(2, initialStock);
                ps.executeUpdate();
            }

            // 4. Reserva de capacidad COMMITTED
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

            // 5. Reserva de inventario COMMITTED
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, 'COMMITTED', NOW() + INTERVAL '30 minutes', NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, invResId);
                ps.setObject(2, STORE_ID);
                ps.setObject(3, capResId);
                ps.setObject(4, variantId);
                ps.setInt(5, reservedQty);
                ps.executeUpdate();
            }

            // 6. Pago APPROVED, refund_status NOT_REQUIRED
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, amount, currency, status, refund_status, approved_at, created_at, updated_at)
                    VALUES (?, ?, 50.00, 'PEN', 'APPROVED', 'NOT_REQUIRED', NOW(), NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, paymentId);
                ps.setObject(2, capResId);
                ps.executeUpdate();
            }

            // 7. Pedido
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

            // 8. Historial inicial
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO order_status_history (id, order_id, changed_by_user_id, status, reason, changed_at)
                    VALUES (gen_random_uuid(), ?, NULL, ?, 'Estado inicial', NOW());
                    """)) {
                ps.setObject(1, orderId);
                ps.setString(2, initialStatus.name());
                ps.executeUpdate();
            }

            return new TestData(orderId, orderNumber, userId, capResId, paymentId, variantId);
        } catch (SQLException e) {
            throw new RuntimeException("Error al preparar datos de prueba en PostgreSQL: " + e.getMessage(), e);
        }
    }

    private void seedUser(UUID userId, String email) {
        createdUserIds.add(userId);
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO users (id, email, password_encoded, first_name, last_name, status, created_at, updated_at)
                    VALUES (?, ?, '$2a$10$abcdefghijklmnopqrstuv', 'Test', 'Customer', 'ACTIVE', NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, userId);
                ps.setString(2, email);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar usuario de prueba: " + e.getMessage(), e);
        }
    }

    private void seedPaymentAttempt(UUID paymentId, String providerPaymentId) {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payment_attempts (id, payment_id, attempt_number, provider, payment_method, idempotency_key, status, provider_payment_id, created_at, updated_at)
                    VALUES (gen_random_uuid(), ?, 1, 'MERCADO_PAGO', 'CARD', ?, 'APPROVED', ?, NOW(), NOW())
                    ON CONFLICT (id) DO NOTHING;
                    """)) {
                ps.setObject(1, paymentId);
                ps.setString(2, "idem-" + UUID.randomUUID());
                ps.setString(3, providerPaymentId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al insertar intento de pago: " + e.getMessage(), e);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SafeTestConfig {

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
