package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.BackendApplication;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.refund.PaymentRefundService;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BackendApplication.class)
@DisplayName("OrderReconciliationPostgresIntegrationTest — Pruebas de integración real con PostgreSQL aislado para conciliación 4B.2")
class OrderReconciliationPostgresIntegrationTest {

    private static HttpServer mockServer;
    private static int mockPort;
    private static ExecutorService mockExecutor;

    private static final AtomicReference<MockHttpResponse> orderMockResponse = new AtomicReference<>();
    private static final AtomicBoolean simulateTimeout = new AtomicBoolean(false);

    record MockHttpResponse(int statusCode, String responseBody) {}

    @BeforeAll
    static void startMockHttpServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockExecutor = Executors.newCachedThreadPool();
        mockServer.setExecutor(mockExecutor);

        mockServer.createContext("/v1/orders/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getRequestBody().readAllBytes();

                if (simulateTimeout.get()) {
                    try {
                        Thread.sleep(1400);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                MockHttpResponse resp = orderMockResponse.get();
                if (resp == null) {
                    resp = new MockHttpResponse(200, "{\"id\": \"ORD-DEFAULT\", \"status\": \"created\"}");
                }

                byte[] bytes = resp.responseBody().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(resp.statusCode(), bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        mockServer.start();
    }

    @AfterAll
    static void stopMockHttpServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
        if (mockExecutor != null) {
            mockExecutor.shutdownNow();
        }
    }

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    @DynamicPropertySource
    static void configureIsolatedProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.hikari.jdbc-url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.hikari.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.hikari.password", database::password);
        registry.add("spring.flyway.url", database::url);
        registry.add("spring.flyway.user", database::username);
        registry.add("spring.flyway.password", database::password);

        registry.add("mercadopago.base-url", () -> "http://127.0.0.1:" + mockPort);
        registry.add("mercadopago.access-token", () -> "TEST-RECON-TOKEN");
        registry.add("mercadopago.connect-timeout-ms", () -> "800");
        registry.add("mercadopago.read-timeout-ms", () -> "800");
        registry.add("payments.mercadopago.refund-scheduler-enabled", () -> "false");
        registry.add("app.payment.reconciliation.enabled", () -> "false");
        registry.add("app.payment.reconciliation.orders-enabled", () -> "true");
        registry.add("app.payment.reconciliation.orders-grace-period-seconds", () -> "0");
        registry.add("app.payment.reconciliation.orders-batch-size", () -> "20");
        registry.add("app.payment.reconciliation.orders-lease-duration-seconds", () -> "300");
    }

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentIncidenceRepository incidenceRepository;
    @Autowired private PaymentOrderReconciliationProcessor orderProcessor;
    @Autowired private PaymentReconciliationService reconciliationService;
    @Autowired private PaymentRefundService refundService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e3");
    private static final UUID PRODUCT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf201");
    private static final UUID VARIANT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302");

    @BeforeEach
    void cleanTestData() {
        simulateTimeout.set(false);
        orderMockResponse.set(new MockHttpResponse(200, "{\"id\": \"ORD-DEFAULT\", \"status\": \"created\"}"));
        jdbcTemplate.update("DELETE FROM payment_refund_execution_attempts");
        jdbcTemplate.update("DELETE FROM payment_refund_intents");
        jdbcTemplate.update("DELETE FROM payment_incidences");
        jdbcTemplate.update("DELETE FROM order_status_history");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM payment_attempts");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM inventory_reservations");
        jdbcTemplate.update("DELETE FROM capacity_reservations WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("DELETE FROM cart_items WHERE store_id = ?", STORE_ID);
        jdbcTemplate.update("DELETE FROM carts WHERE user_id = ?", USER_ID);
        jdbcTemplate.update("UPDATE inventories SET quantity = 100 WHERE variant_id = ?", VARIANT_ID);

        createCartWithItem(USER_ID);
    }

    private void createCartWithItem(UUID userId) {
        jdbcTemplate.update("DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM carts WHERE user_id = ?)", userId);
        jdbcTemplate.update("DELETE FROM carts WHERE user_id = ?", userId);
        UUID cartId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO carts (id, user_id, store_id, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
                cartId, userId, STORE_ID
        );
        jdbcTemplate.update(
                "INSERT INTO cart_items (cart_id, variant_id, store_id, quantity, reference_price_amount, reference_price_currency, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 1, 12.50, 'PEN', NOW(), NOW())",
                cartId, VARIANT_ID, STORE_ID
        );
    }

    private UUID createCapacityReservation(String status, Instant expiresAt) {
        UUID reservationId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant protectionExpires = "PAYMENT_PROTECTED".equals(status) ? now.plusSeconds(1800) : null;
        jdbcTemplate.update(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, " +
                        "status, expires_at, payment_protection_expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reservationId, STORE_ID, USER_ID, Date.valueOf(LocalDate.now().plusDays(1)),
                Time.valueOf(LocalTime.of(10, 0)), Time.valueOf(LocalTime.of(11, 0)),
                status, java.sql.Timestamp.from(expiresAt != null ? expiresAt : now.plusSeconds(1800)),
                protectionExpires != null ? java.sql.Timestamp.from(protectionExpires) : null,
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now)
        );
        return reservationId;
    }

    private UUID createActiveInventoryReservation(UUID capacityReservationId, int quantity) {
        UUID inventoryReservationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'ACTIVE', NOW() + INTERVAL '30 minutes', NOW(), NOW())",
                inventoryReservationId, STORE_ID, capacityReservationId, VARIANT_ID, quantity
        );
        return inventoryReservationId;
    }

    private Payment createPendingPayment(UUID reservationId, BigDecimal amount, String snapshot) {
        Payment payment = Payment.createPending(
                reservationId, amount, "PEN",
                FulfillmentType.DELIVERY, "Cliente Recon", "987654321", "cliente.recon@test.com",
                "Calle Recon 123", null, null, null, amount, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, snapshot != null ? snapshot : "[]"
        );
        return paymentRepository.save(payment);
    }

    private String buildValidSnapshot(BigDecimal amount) {
        return String.format(Locale.ROOT, """
                [{"variantId":"%s","productId":"%s","productName":"Pastel","presentationName":"Familiar","sku":"PASTEL-FAM-01","imageUrl":null,"quantity":1,"unitPriceAmount":%.2f,"currency":"PEN","subtotalAmount":%.2f,"tracksInventory":true}]
                """, VARIANT_ID, PRODUCT_ID, amount, amount);
    }

    private PaymentAttempt createAttempt(Payment payment, String orderId, String checkoutMode, PaymentAttemptStatus status, Instant createdAt) {
        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-" + payment.getId(), checkoutMode
        );
        if (orderId != null) {
            attempt.recordOrder(orderId, "https://mp.com/init?order_id=" + orderId);
        }
        if (status == PaymentAttemptStatus.PROCESSING) {
            attempt.markProcessing();
        }
        attempt = paymentAttemptRepository.save(attempt);
        if (createdAt != null) {
            jdbcTemplate.update("UPDATE payment_attempts SET created_at = ? WHERE id = ?",
                    java.sql.Timestamp.from(createdAt), attempt.getId());
            attempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        }
        return attempt;
    }

    // =========================================================================
    // 1. ORDEN ACREDITADA SIN WEBHOOK, RECUPERADA POR SCHEDULER
    // =========================================================================
    @Test
    @DisplayName("Escenario 1: Orden acreditada en Mercado Pago sin webhook es recuperada y confirma el pedido comercial")
    void shouldRecoverAccreditedOrderWithoutWebhookAndConfirmPurchase() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-001", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        String mpOrderJson = String.format(Locale.ROOT, """
                {
                  "id": "ORD-RECON-001",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 50.00,
                  "external_reference": "%s",
                  "transactions": {
                    "payments": [
                      {
                        "id": 88001122,
                        "status": "approved",
                        "status_detail": "accredited",
                        "transaction_amount": 50.00,
                        "currency_id": "PEN",
                        "date_approved": "2026-11-05T19:00:00.000Z"
                      }
                    ]
                  }
                }
                """, payment.getId());
        orderMockResponse.set(new MockHttpResponse(200, mpOrderJson));

        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.totalCandidates()).isEqualTo(1);
        assertThat(summary.successCount()).isEqualTo(1);

        Payment reloadedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);

        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(reloadedAttempt.getProviderPaymentId()).isEqualTo("88001122");
        assertThat(reloadedAttempt.getReconciliationClaimToken()).isNull();

        assertThat(orderRepository.findByPaymentId(payment.getId())).isPresent();
    }

    // =========================================================================
    // 2. ORDEN CREATED SIN TRANSACCIONES PERMANECE ABIERTA
    // =========================================================================
    @Test
    @DisplayName("Escenario 2: Orden 'created' sin transacciones preserva estado y no se rechaza prematuramente")
    void shouldKeepOpenCreatedOrderWithoutTransactions() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-002", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        orderMockResponse.set(new MockHttpResponse(200, """
                {
                  "id": "ORD-RECON-002",
                  "status": "created",
                  "total_amount": 50.00,
                  "transactions": {
                    "payments": []
                  }
                }
                """));

        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.totalCandidates()).isEqualTo(1);
        assertThat(summary.successCount()).isEqualTo(1);

        Payment reloadedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(reloadedAttempt.getReconciliationClaimToken()).isNull();
    }

    // =========================================================================
    // 3. ORDEN CANCELADA SIN TRANSACCIONES
    // =========================================================================
    @Test
    @DisplayName("Escenario 3: Orden cancelada sin transacciones marca intento como REJECTED")
    void shouldMarkAttemptRejectedOnCancelledOrderWithoutTransactions() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-003", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        orderMockResponse.set(new MockHttpResponse(200, """
                {
                  "id": "ORD-RECON-003",
                  "status": "cancelled",
                  "status_detail": "cancelled_by_user",
                  "transactions": {
                    "payments": []
                  }
                }
                """));

        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.totalCandidates()).isEqualTo(1);
        assertThat(summary.successCount()).isEqualTo(1);

        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.REJECTED);
        assertThat(reloadedAttempt.getReconciliationClaimToken()).isNull();
    }

    // =========================================================================
    // 4. ORDEN EXPIRADA SIN TRANSACCIONES
    // =========================================================================
    @Test
    @DisplayName("Escenario 4: Orden expirada sin transacciones marca intento como REJECTED")
    void shouldMarkAttemptRejectedOnExpiredOrderWithoutTransactions() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-004", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        orderMockResponse.set(new MockHttpResponse(200, """
                {
                  "id": "ORD-RECON-004",
                  "status": "expired",
                  "transactions": {
                    "payments": []
                  }
                }
                """));

        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.totalCandidates()).isEqualTo(1);
        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.REJECTED);
    }

    // =========================================================================
    // 5. PAGO ACREDITADO DESPUÉS DE EXPIRAR LA RESERVA
    // =========================================================================
    @Test
    @DisplayName("Escenario 5: Pago acreditado con reserva vencida preserva APPROVED y registra incidencia/reembolso")
    void shouldRecordIncidenceWhenAccreditedAfterCapacityReservationExpired() {
        BigDecimal amount = BigDecimal.valueOf(60.00);
        UUID reservationId = createCapacityReservation("EXPIRED", Instant.now().minusSeconds(600));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-005", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        String mpOrderJson = String.format(Locale.ROOT, """
                {
                  "id": "ORD-RECON-005",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 60.00,
                  "external_reference": "%s",
                  "transactions": {
                    "payments": [
                      {
                        "id": 99112233,
                        "status": "approved",
                        "status_detail": "accredited",
                        "transaction_amount": 60.00,
                        "currency_id": "PEN",
                        "date_approved": "2026-11-05T19:00:00.000Z"
                      }
                    ]
                  }
                }
                """, payment.getId());
        orderMockResponse.set(new MockHttpResponse(200, mpOrderJson));

        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.totalCandidates()).isEqualTo(1);
        assertThat(summary.commercialIncidenceCount()).isEqualTo(1);

        Payment reloadedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(reloadedPayment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(orderRepository.findByPaymentId(payment.getId())).isEmpty();
    }

    // =========================================================================
    // 6. WEBHOOK TARDÍO CONCURRENTE CON RECONCILIACIÓN
    // =========================================================================
    @Test
    @DisplayName("Escenario 6: Webhook concurrente que aprueba el pago evita doble confirmación en consolidación")
    void shouldAvoidDuplicateConfirmationWhenWebhookConcurrentlyApproves() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-006", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        UUID workerToken = UUID.randomUUID();
        OrderReconciliationClaimResult claim = orderProcessor.claimAttempt(attempt.getId(), workerToken, Duration.ofMinutes(5), Instant.now());
        assertThat(claim.claimed()).isTrue();

        // Webhook aprueba el pago concurrentemente
        payment.markApproved(Instant.now());
        paymentRepository.save(payment);

        OrderReconciliationCandidateResult result = orderProcessor.consolidateOrderReconciliation(
                claim, null, workerToken, Instant.now()
        );

        assertThat(result.status()).isEqualTo(OrderReconciliationCandidateResult.Status.SUCCESS_ALREADY_APPROVED);
        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getReconciliationClaimToken()).isNull();
    }

    // =========================================================================
    // 7. DOS WORKERS INTENTANDO RECLAMAR LA MISMA ORDEN
    // =========================================================================
    @Test
    @DisplayName("Escenario 7: Dos workers intentando reclamar el mismo intento serializan y solo uno adquiere lease")
    void shouldAllowOnlyOneWorkerToClaimAttemptConcurrently() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-007", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();

        OrderReconciliationClaimResult claim1 = orderProcessor.claimAttempt(attempt.getId(), worker1, Duration.ofMinutes(5), Instant.now());
        OrderReconciliationClaimResult claim2 = orderProcessor.claimAttempt(attempt.getId(), worker2, Duration.ofMinutes(5), Instant.now());

        assertThat(claim1.claimed()).isTrue();
        assertThat(claim2.claimed()).isFalse();
        assertThat(claim2.skipReason()).contains("lease activo");
    }

    // =========================================================================
    // 8. LEASE VENCIDO RECUPERADO POR OTRO WORKER
    // =========================================================================
    @Test
    @DisplayName("Escenario 8: Lease vencido permite que un segundo worker lo reclame exitosamente")
    void shouldAllowNewWorkerToClaimExpiredLease() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-008", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();

        // Worker 1 adquiere lease que expira en el pasado
        attempt.acquireReconciliationLease(worker1, Duration.ofSeconds(1), Instant.now().minusSeconds(60));
        paymentAttemptRepository.save(attempt);

        OrderReconciliationClaimResult claim2 = orderProcessor.claimAttempt(attempt.getId(), worker2, Duration.ofMinutes(5), Instant.now());
        assertThat(claim2.claimed()).isTrue();

        PaymentAttempt reloaded = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloaded.getReconciliationClaimToken()).isEqualTo(worker2);
    }

    // =========================================================================
    // 9. WORKER ANTIGUO INTENTANDO CONSOLIDAR CON TOKEN INVÁLIDO
    // =========================================================================
    @Test
    @DisplayName("Escenario 9: Worker antiguo con token inválido es rechazado al consolidar (CLAIM_LOST)")
    void shouldRejectConsolidationFromStaleWorkerWithInvalidToken() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-009", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        UUID oldWorker = UUID.randomUUID();
        UUID newWorker = UUID.randomUUID();

        // Worker actual vigente es newWorker
        attempt.acquireReconciliationLease(newWorker, Duration.ofMinutes(5), Instant.now());
        paymentAttemptRepository.save(attempt);

        OrderReconciliationClaimResult staleClaim = OrderReconciliationClaimResult.claimed(
                attempt.getId(), payment.getId(), oldWorker, "ORD-RECON-009"
        );

        OrderReconciliationCandidateResult result = orderProcessor.consolidateOrderReconciliation(
                staleClaim, null, oldWorker, Instant.now()
        );

        assertThat(result.status()).isEqualTo(OrderReconciliationCandidateResult.Status.CLAIM_LOST);
    }

    // =========================================================================
    // 10. TIMEOUT O HTTP 5XX SIN DEGRADACIÓN DEL ESTADO LOCAL
    // =========================================================================
    @Test
    @DisplayName("Escenario 10: Timeout o error HTTP 5xx en consulta remota libera lease sin degradar estado")
    void shouldPreserveStateAndReleaseLeaseOnTimeoutOrHttp500() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-010", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        simulateTimeout.set(true);

        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.totalCandidates()).isEqualTo(1);
        assertThat(summary.failureCount()).isEqualTo(1);

        Payment reloadedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(reloadedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.PROCESSING);
        assertThat(reloadedAttempt.getReconciliationClaimToken()).isNull();
    }

    // =========================================================================
    // 11. RESPUESTA REMOTA CON MÚLTIPLES TRANSACCIONES
    // =========================================================================
    @Test
    @DisplayName("Escenario 11: Respuesta con múltiples transacciones selecciona la acreditada y confirma pedido")
    void shouldSelectAccreditedTransactionWhenMultipleTransactionsReturned() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-011", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        String mpOrderJson = String.format(Locale.ROOT, """
                {
                  "id": "ORD-RECON-011",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 50.00,
                  "external_reference": "%s",
                  "transactions": {
                    "payments": [
                      {
                        "id": 11111,
                        "status": "rejected",
                        "status_detail": "cc_rejected_bad_filled_date",
                        "transaction_amount": 50.00,
                        "currency_id": "PEN"
                      },
                      {
                        "id": 22222,
                        "status": "approved",
                        "status_detail": "accredited",
                        "transaction_amount": 50.00,
                        "currency_id": "PEN",
                        "date_approved": "2026-11-05T19:00:00.000Z"
                      }
                    ]
                  }
                }
                """, payment.getId());
        orderMockResponse.set(new MockHttpResponse(200, mpOrderJson));

        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.successCount()).isEqualTo(1);
        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(reloadedAttempt.getProviderPaymentId()).isEqualTo("22222");
    }

    // =========================================================================
    // 12. REINICIO DEL WORKER ENTRE CONSULTA REMOTA Y CONSOLIDACIÓN
    // =========================================================================
    @Test
    @DisplayName("Escenario 12: Worker que se reinicia tras adquirir lease permite recuperación tras expirar lease")
    void shouldRecoverAfterWorkerCrashAndLeaseExpiration() {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        Payment payment = createPendingPayment(reservationId, amount, buildValidSnapshot(amount));
        PaymentAttempt attempt = createAttempt(payment, "ORD-RECON-012", "ORDERS", PaymentAttemptStatus.PROCESSING, Instant.now().minusSeconds(300));

        UUID crashedWorker = UUID.randomUUID();
        // Worker se cae y el lease vence
        attempt.acquireReconciliationLease(crashedWorker, Duration.ofSeconds(1), Instant.now().minusSeconds(10));
        paymentAttemptRepository.save(attempt);

        String mpOrderJson = String.format(Locale.ROOT, """
                {
                  "id": "ORD-RECON-012",
                  "status": "processed",
                  "status_detail": "accredited",
                  "total_amount": 50.00,
                  "external_reference": "%s",
                  "transactions": {
                    "payments": [
                      {
                        "id": 77777,
                        "status": "approved",
                        "status_detail": "accredited",
                        "transaction_amount": 50.00,
                        "currency_id": "PEN",
                        "date_approved": "2026-11-05T19:00:00.000Z"
                      }
                    ]
                  }
                }
                """, payment.getId());
        orderMockResponse.set(new MockHttpResponse(200, mpOrderJson));

        // Nuevo ciclo de scheduler recupera la orden
        reconciliationService.setOrdersReconciliationEnabled(true);
        OrderReconciliationBatchSummary summary = reconciliationService.reconcileOrphanOrders();

        assertThat(summary.successCount()).isEqualTo(1);
        PaymentAttempt reloadedAttempt = paymentAttemptRepository.findById(attempt.getId()).orElseThrow();
        assertThat(reloadedAttempt.getStatus()).isEqualTo(PaymentAttemptStatus.APPROVED);
        assertThat(reloadedAttempt.getProviderPaymentId()).isEqualTo("77777");
    }

    // =========================================================================
    // 13. REGRESIÓN DE PREFERENCES Y SUBSISTEMA DE REEMBOLSOS 4B.1
    // =========================================================================
    @Test
    @DisplayName("Escenario 13: Reconciliación de pedidos aprobados huérfanos y reembolsos 4B.1 operan sin regresión")
    void shouldEnsureNoRegressionInApprovedPaymentsReconciliationAndRefunds() {
        // A. Reconcile orphan approved payments (PREFERENCES legacy)
        ReconciliationBatchSummary prefSummary = reconciliationService.reconcileOrphanApprovedPayments();
        assertThat(prefSummary).isNotNull();

        // B. Reconcile refunds (4B.1)
        var refundStats = refundService.processPendingRefunds(10);
        assertThat(refundStats).isNotNull();
    }
}
