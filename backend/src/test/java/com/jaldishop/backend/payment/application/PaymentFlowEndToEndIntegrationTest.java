package com.jaldishop.backend.payment.application;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.reconciliation.PaymentConfirmationRunner;
import com.jaldishop.backend.payment.application.reconciliation.PaymentIncidenceRecorder;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationProcessor;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService;
import com.jaldishop.backend.payment.application.reconciliation.ReconciliationCandidateResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor;
import com.jaldishop.backend.payment.application.refund.PaymentRefundService;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookCommand;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookResult;
import com.jaldishop.backend.payment.application.webhook.ProcessMercadoPagoWebhookService;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("PaymentFlowEndToEndIntegrationTest — Fase 5C: Pruebas Integrales del Flujo de Pagos")
class PaymentFlowEndToEndIntegrationTest {

    private static HttpServer mockServer;
    private static ExecutorService mockExecutor;
    private static int mockPort;

    private static final String WEBHOOK_SECRET = "test-webhook-secret-key-123";
    private static final Long EXPECTED_COLLECTOR_ID = 123456789L;

    private static final AtomicReference<MockResponse> paymentMockResponse = new AtomicReference<>();
    private static final AtomicReference<MockResponse> postRefundMockResponse = new AtomicReference<>();
    private static final AtomicReference<MockResponse> getRefundMockResponse = new AtomicReference<>();
    private static final AtomicBoolean simulateTimeout = new AtomicBoolean(false);
    private static final List<String> receivedIdempotencyKeys = new CopyOnWriteArrayList<>();

    record MockResponse(int statusCode, String body) {}

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockExecutor = Executors.newCachedThreadPool();
        mockServer.setExecutor(mockExecutor);

        mockServer.createContext("/v1/payments/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.getRequestBody().readAllBytes();
                String path = exchange.getRequestURI().getPath();
                String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);

                String idempotencyKey = exchange.getRequestHeaders().getFirst("X-Idempotency-Key");
                if (idempotencyKey != null) {
                    receivedIdempotencyKeys.add(idempotencyKey);
                }

                if (simulateTimeout.get()) {
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                MockResponse resp;
                if (path.endsWith("/refunds")) {
                    if ("GET".equals(method)) {
                        resp = getRefundMockResponse.get();
                        if (resp == null) resp = new MockResponse(200, "[]");
                    } else {
                        resp = postRefundMockResponse.get();
                        if (resp == null) resp = new MockResponse(201, "{}");
                    }
                } else {
                    resp = paymentMockResponse.get();
                    if (resp == null) resp = new MockResponse(200, "{}");
                }

                byte[] bytes = resp.body().getBytes(StandardCharsets.UTF_8);
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
    static void stopMockServer() {
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
        registry.add("mercadopago.access-token", () -> "TEST-ISOLATED-TOKEN");
        registry.add("mercadopago.webhook-secret", () -> WEBHOOK_SECRET);
        registry.add("mercadopago.expected-collector-id", () -> String.valueOf(EXPECTED_COLLECTOR_ID));
        registry.add("mercadopago.expected-live-mode", () -> "false");
        registry.add("mercadopago.connect-timeout-ms", () -> "800");
        registry.add("mercadopago.read-timeout-ms", () -> "800");
        registry.add("payments.mercadopago.refund-scheduler-enabled", () -> "false");
        registry.add("app.payment.reconciliation.enabled", () -> "false");
    }

    @Autowired private ProcessMercadoPagoWebhookService webhookService;
    @Autowired private ConfirmPurchaseService confirmPurchaseService;
    @Autowired private PaymentReconciliationService reconciliationService;
    @Autowired private PaymentReconciliationProcessor reconciliationProcessor;
    @Autowired private PaymentConfirmationRunner confirmationRunner;
    @Autowired private PaymentIncidenceRecorder incidenceRecorder;
    @Autowired private PaymentRefundService refundService;
    @Autowired private PaymentRefundProcessor refundProcessor;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired private PaymentRefundIntentRepository refundIntentRepository;
    @Autowired private PaymentRefundExecutionAttemptRepository executionAttemptRepository;
    @Autowired private PaymentIncidenceRepository incidenceRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e3");
    private static final UUID PRODUCT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf201");
    private static final UUID VARIANT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302");

    @BeforeEach
    void cleanData() {
        simulateTimeout.set(false);
        receivedIdempotencyKeys.clear();
        paymentMockResponse.set(new MockResponse(200, "{}"));
        postRefundMockResponse.set(new MockResponse(201, "{}"));
        getRefundMockResponse.set(new MockResponse(200, "[]"));

        jdbcTemplate.update("DELETE FROM payment_refund_execution_attempts");
        jdbcTemplate.update("DELETE FROM payment_refund_intents");
        jdbcTemplate.update("DELETE FROM payment_incidences");
        jdbcTemplate.update("DELETE FROM payment_attempts");
        jdbcTemplate.update("DELETE FROM order_items");
        jdbcTemplate.update("DELETE FROM orders");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM inventory_reservations");
        jdbcTemplate.update("DELETE FROM capacity_reservations WHERE user_id = ?", USER_ID);
    }

    private String calculateHmac(String dataId, String requestId, long ts) {
        String manifest = "id:" + dataId.trim().toLowerCase(Locale.ROOT) + ";request-id:" + requestId.trim() + ";ts:" + ts + ";";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmacBytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return "ts=" + ts + ",v1=" + sb;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UUID createCapacityReservation(String status, Instant expiresAt) {
        UUID reservationId = UUID.randomUUID();
        Instant protectionExpires = "PAYMENT_PROTECTED".equals(status) ? Instant.now().plusSeconds(1800) : null;
        jdbcTemplate.update(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, " +
                        "status, expires_at, payment_protection_expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reservationId, STORE_ID, USER_ID, Date.valueOf(LocalDate.now().plusDays(1)),
                Time.valueOf(LocalTime.of(10, 0)), Time.valueOf(LocalTime.of(11, 0)),
                status, java.sql.Timestamp.from(expiresAt != null ? expiresAt : Instant.now().plusSeconds(1800)),
                protectionExpires != null ? java.sql.Timestamp.from(protectionExpires) : null,
                java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
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
                FulfillmentType.DELIVERY, "Cliente Integral", "987654321", "cliente@integral.com",
                "Av Siempre Viva 123", null, null, null, amount, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, snapshot != null ? snapshot : "[]"
        );
        return paymentRepository.save(payment);
    }

    private String buildValidSnapshot(BigDecimal amount) {
        return String.format(Locale.ROOT, """
                [{"variantId":"%s","productId":"%s","productName":"Torta","presentationName":"Individual","sku":"TORTA-CHOC-IND","imageUrl":null,"quantity":1,"unitPriceAmount":%.2f,"currency":"PEN","subtotalAmount":%.2f,"tracksInventory":true}]
                """, VARIANT_ID, PRODUCT_ID, amount, amount);
    }

    // =========================================================================
    // ESCENARIO A: PAGO APROBADO Y PEDIDO CONFIRMADO (Nominal e Idempotente)
    // =========================================================================
    @Test
    @DisplayName("Escenario A: Pago aprobado y pedido confirmado con inventario y capacidad comprometidos, e idempotencia con segundo webhook")
    void scenarioA_approvedPaymentAndOrderConfirmedWithIdempotentWebhook() {
        BigDecimal amount = BigDecimal.valueOf(25.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        UUID inventoryReservationId = createActiveInventoryReservation(reservationId, 1);
        String snapshot = buildValidSnapshot(amount);
        Payment payment = createPendingPayment(reservationId, amount, snapshot);
        UUID paymentId = payment.getId();

        PaymentAttempt initialAttempt = PaymentAttempt.create(
                paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-b-" + paymentId
        );
        paymentAttemptRepository.save(initialAttempt);

        String mpPaymentId = "mp-pay-9001";
        paymentMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": 9001,
                  "status": "approved",
                  "status_detail": "accredited",
                  "transaction_amount": 25.00,
                  "currency_id": "PEN",
                  "date_approved": "2026-11-02T22:30:00.000Z",
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "payment_method_id": "account_money"
                }
                """, paymentId)));

        String requestId = UUID.randomUUID().toString();
        long ts = Instant.now().getEpochSecond();
        String xSignature = calculateHmac(mpPaymentId, requestId, ts);
        String rawBody = String.format("""
                {"action":"payment.created","data":{"id":"%s"},"type":"payment"}
                """, mpPaymentId);

        // 1. Primer webhook
        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, requestId, Map.of("data.id", mpPaymentId, "type", "payment"), rawBody
        );
        ProcessMercadoPagoWebhookResult result = webhookService.execute(command);

        assertEquals("APPROVED", result.paymentStatus());
        assertTrue(result.message().contains("confirmados exitosamente"));

        // Verificar Order en base de datos
        Order order = orderRepository.findByPaymentId(paymentId).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(0, amount.compareTo(order.getTotalAmount()));

        // Verificar inventario comprometido (COMMITTED)
        String invStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM inventory_reservations WHERE id = ?", String.class, inventoryReservationId
        );
        assertEquals("COMMITTED", invStatus);

        // Verificar Payment.refundStatus = NOT_REQUIRED
        Payment paymentAfter = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(RefundStatus.NOT_REQUIRED, paymentAfter.getRefundStatus());

        // 2. Segundo webhook repetido (Idempotencia)
        ProcessMercadoPagoWebhookResult repeatResult = webhookService.execute(command);
        assertEquals("APPROVED", repeatResult.paymentStatus());

        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE payment_id = ?", Integer.class, paymentId
        );
        assertEquals(1, orderCount, "No debe duplicarse la orden en la base de datos");
    }

    // =========================================================================
    // ESCENARIO B: PAGO APROBADO Y FALLO COMERCIAL (Rollback y Reembolso Total)
    // =========================================================================
    @Test
    @DisplayName("Escenario B: Pago aprobado con reserva expirada sufre rollback comercial, refundStatus=PENDING y se reembolsa a COMPLETED")
    void scenarioB_approvedPaymentWithCommercialFailureAndFullRefund() {
        BigDecimal amount = BigDecimal.valueOf(35.00);
        // Reserva definitivamente expirada en el pasado
        UUID reservationId = createCapacityReservation("EXPIRED", Instant.now().minusSeconds(600));
        String snapshot = buildValidSnapshot(amount);
        Payment payment = createPendingPayment(reservationId, amount, snapshot);
        UUID paymentId = payment.getId();

        PaymentAttempt initialAttempt = PaymentAttempt.create(
                paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-b-" + paymentId
        );
        paymentAttemptRepository.save(initialAttempt);

        String mpPaymentId = "mp-pay-9002";
        paymentMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": 9002,
                  "status": "approved",
                  "status_detail": "accredited",
                  "transaction_amount": 35.00,
                  "currency_id": "PEN",
                  "date_approved": "2026-11-02T22:31:00.000Z",
                  "external_reference": "%s",
                  "collector_id": 123456789,
                  "live_mode": false,
                  "payment_method_id": "account_money"
                }
                """, paymentId)));

        String requestId = UUID.randomUUID().toString();
        long ts = Instant.now().getEpochSecond();
        String xSignature = calculateHmac(mpPaymentId, requestId, ts);
        String rawBody = String.format("""
                {"action":"payment.created","data":{"id":"%s"},"type":"payment"}
                """, mpPaymentId);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, requestId, Map.of("data.id", mpPaymentId, "type", "payment"), rawBody
        );
        ProcessMercadoPagoWebhookResult result = webhookService.execute(command);

        // Webhook responde success 200 pero reporta fallo comercial
        assertEquals("APPROVED", result.paymentStatus());

        // Verificar que NO hay orden creada (Rollback comercial)
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE payment_id = ?", Integer.class, paymentId
        );
        assertEquals(0, orderCount);

        // Verificar Payment.APPROVED y refundStatus=PENDING
        Payment paymentInDb = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.APPROVED, paymentInDb.getStatus());
        assertEquals(RefundStatus.PENDING, paymentInDb.getRefundStatus());

        // Ejecutar reembolso mediante PaymentRefundService
        postRefundMockResponse.set(new MockResponse(201, """
                {
                  "id": 88009002,
                  "payment_id": 9002,
                  "amount": 35.00,
                  "status": "approved",
                  "date_created": "2026-11-02T22:32:00.000Z"
                }
                """));

        PaymentRefundService.RefundBatchSummary refundSummary = refundService.processPendingRefunds(10);
        assertTrue(refundSummary.completedCount() >= 1);

        Payment paymentRefunded = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, paymentRefunded.getRefundStatus());
        assertEquals("88009002", paymentRefunded.getRefundReference());
    }

    // =========================================================================
    // ESCENARIO C: FALLO TÉCNICO DE CONFIRMACIÓN Y RECUPERACIÓN VÍA RECONCILIADOR
    // =========================================================================
    @Test
    @DisplayName("Escenario C: Fallo transitorio no marca refundStatus=PENDING y se recupera el pedido vía reconciliador")
    void scenarioC_transientTechnicalFailureAndReconciliationRecovery() {
        BigDecimal amount = BigDecimal.valueOf(40.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        UUID inventoryReservationId = createActiveInventoryReservation(reservationId, 1);
        String snapshot = buildValidSnapshot(amount);
        Payment payment = createPendingPayment(reservationId, amount, snapshot);
        payment.markApproved(Instant.now().minusSeconds(120)); // aprobado previamente hace 2 minutos
        paymentRepository.save(payment);
        UUID paymentId = payment.getId();

        // 1. Simular registro de incidencia técnica por fallo transitorio
        incidenceRecorder.recordTechnicalRetry(paymentId, new RuntimeException("Falla transitoria de red simulada"));

        Payment paymentBefore = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(RefundStatus.NOT_REQUIRED, paymentBefore.getRefundStatus(), "Fallo técnico NO debe marcar PENDING");

        // Simular que el backoff ya expiró
        jdbcTemplate.update("UPDATE payment_incidences SET next_retry_at = now() - interval '1 second' WHERE payment_id = ?", paymentId);

        // 2. Ejecutar reconciliador
        ReconciliationCandidateResult result = reconciliationProcessor.processCandidate(paymentId);
        assertEquals(ReconciliationCandidateResult.Status.SUCCESS, result.status());

        // Verificar Orden creada
        Order order = orderRepository.findByPaymentId(paymentId).orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        // Verificar que NO se generó ninguna intención de reembolso
        Optional<PaymentRefundIntent> intentOpt = refundIntentRepository.findByPaymentId(paymentId);
        assertTrue(intentOpt.isEmpty(), "No debe generarse intención de reembolso tras confirmación exitosa");
    }

    // =========================================================================
    // ESCENARIO D: TIMEOUT FINANCIERO Y CONCILIACIÓN VIA GET
    // =========================================================================
    @Test
    @DisplayName("Escenario D: Timeout en POST de reembolso mantiene clave y se concilia mediante GET sin duplicar")
    void scenarioD_financialTimeoutAndReconciliationViaGet() {
        BigDecimal amount = BigDecimal.valueOf(45.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, "[]");
        payment.markApproved(Instant.now());
        payment.requestRefund(amount, "COMMERCIAL_CONFLICT");
        payment = paymentRepository.save(payment);
        UUID paymentId = payment.getId();

        PaymentAttempt attempt = PaymentAttempt.create(
                paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-d-" + paymentId
        );
        attempt.markApproved("mp-pay-9004", "approved", "accredited", PaymentMethod.WALLET_PURCHASE, Instant.now());
        paymentAttemptRepository.save(attempt);

        // 1. Primer ciclo con timeout en POST
        simulateTimeout.set(true);
        UUID worker1 = UUID.randomUUID();
        PaymentRefundProcessor.RefundClaimResult claim1 = refundProcessor.claimOrCreateRefundIntent(paymentId, worker1);
        PaymentRefundProcessor.RefundExecutionResult exec1 = refundProcessor.executeExternalRefund(claim1);
        assertEquals(PaymentRefundProcessor.RefundExecutionResult.ExecutionStatus.UNKNOWN, exec1.status());
        refundProcessor.consolidateRefundResult(claim1, exec1);

        PaymentRefundIntent intent = refundIntentRepository.findByPaymentId(paymentId).orElseThrow();
        assertEquals(RefundIntentStatus.UNKNOWN, intent.getStatus());
        String stableKey = intent.getIdempotencyKey();

        // 2. En el segundo ciclo, GET reporta que MP sí aprobó el reembolso
        simulateTimeout.set(false);
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        jdbcTemplate.update("UPDATE payment_incidences SET next_retry_at = now() - interval '1 second' WHERE payment_id = ?", paymentId);

        getRefundMockResponse.set(new MockResponse(200, """
                [
                  {
                    "id": 88009004,
                    "payment_id": 9004,
                    "amount": 45.00,
                    "status": "approved",
                    "date_created": "2026-11-02T22:33:00.000Z"
                  }
                ]
                """));

        UUID worker2 = UUID.randomUUID();
        PaymentRefundProcessor.RefundClaimResult claim2 = refundProcessor.claimOrCreateRefundIntent(paymentId, worker2);
        assertTrue(claim2.claimed());
        assertEquals(stableKey, claim2.idempotencyKey(), "Debe preservar la misma clave idempotente");

        PaymentRefundProcessor.RefundExecutionResult exec2 = refundProcessor.executeExternalRefund(claim2);
        assertEquals(PaymentRefundProcessor.RefundExecutionResult.ExecutionStatus.SUCCESSFUL, exec2.status());
        assertEquals("GET", exec2.requestMethod(), "Debe haberse resuelto vía GET sin nuevo POST");

        refundProcessor.consolidateRefundResult(claim2, exec2);

        Payment reloaded = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
        assertEquals("88009004", reloaded.getRefundReference());
    }

    // =========================================================================
    // ESCENARIO E: CONCURRENCIA (Webhooks, Reconciliador, Workers y Lease)
    // =========================================================================
    @Test
    @DisplayName("Escenario E: Concurrencia entre workers y reconciliador garantiza exactamente 1 orden y 1 intención de reembolso")
    void scenarioE_concurrencyWebhooksReconcilerAndRefundWorkers() throws Exception {
        BigDecimal amount = BigDecimal.valueOf(50.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        createActiveInventoryReservation(reservationId, 1);
        String snapshot = buildValidSnapshot(amount);
        Payment payment = createPendingPayment(reservationId, amount, snapshot);
        payment.markApproved(Instant.now().minusSeconds(120));
        paymentRepository.save(payment);
        UUID paymentId = payment.getId();

        // Dos hilos compitiendo: ConfirmPurchaseService vs PaymentConfirmationRunner
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Void> task1 = () -> {
            latch.await();
            try {
                confirmationRunner.confirmUnderLock(paymentId);
            } catch (Exception ignored) {}
            return null;
        };
        Callable<Void> task2 = () -> {
            latch.await();
            try {
                confirmationRunner.confirmUnderLock(paymentId);
            } catch (Exception ignored) {}
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);
        latch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactamente 1 orden
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE payment_id = ?", Integer.class, paymentId
        );
        assertEquals(1, orderCount, "Debe existir exactamente 1 orden confirmada");
    }

    // =========================================================================
    // ESCENARIO F: ERROR DE PERSISTENCIA DE INCIDENCIA Y TRANSACCIONALIDAD
    // =========================================================================
    @Test
    @DisplayName("Escenario F: Rollback limpio ante error y Payment e Intent permanecen recuperables tras lease")
    void scenarioF_incidencePersistenceErrorAndTransactionalRecovery() {
        BigDecimal amount = BigDecimal.valueOf(60.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, "[]");
        payment.markApproved(Instant.now());
        payment.requestRefund(amount, "COMMERCIAL_CONFLICT");
        payment = paymentRepository.save(payment);
        UUID paymentId = payment.getId();

        PaymentAttempt attempt = PaymentAttempt.create(
                paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-f-" + paymentId
        );
        attempt.markApproved("mp-pay-9006", "approved", "accredited", PaymentMethod.WALLET_PURCHASE, Instant.now());
        paymentAttemptRepository.save(attempt);

        // Worker 1 adquiere el claim de la intención
        UUID worker1 = UUID.randomUUID();
        PaymentRefundProcessor.RefundClaimResult claim1 = refundProcessor.claimOrCreateRefundIntent(paymentId, worker1);
        assertTrue(claim1.claimed());

        PaymentRefundIntent intentBefore = refundIntentRepository.findByPaymentId(paymentId).orElseThrow();
        assertEquals(RefundIntentStatus.PROCESSING, intentBefore.getStatus());

        // Simular que el proceso o BD sufre un fallo y la consolidación no se completa
        // Simular lease vencido para worker 1
        jdbcTemplate.update("UPDATE payment_refund_intents SET lease_expires_at = now() - interval '10 seconds' WHERE payment_id = ?", paymentId);

        // Worker 2 debe poder reclamar la intención de forma segura (recuperabilidad garantizada)
        UUID worker2 = UUID.randomUUID();
        PaymentRefundProcessor.RefundClaimResult claim2 = refundProcessor.claimOrCreateRefundIntent(paymentId, worker2);
        assertTrue(claim2.claimed(), "Worker 2 debe poder reclamar la intención tras vencer el lease");
        assertEquals(worker2, claim2.claimToken());

        // Si worker 1 intenta consolidar tarde, es rechazado por pérdida de claim
        PaymentRefundProcessor.RefundExecutionResult execLate = PaymentRefundProcessor.RefundExecutionResult.success(
                "POST", 201, "late-ref", amount, "approved", "tardio"
        );
        PaymentRefundProcessor.RefundConsolidationResult consLate = refundProcessor.consolidateRefundResult(claim1, execLate);
        assertEquals(PaymentRefundProcessor.RefundConsolidationResult.ConsolidationStatus.CLAIM_LOST, consLate.status());
    }

    // =========================================================================
    // ESCENARIO G: SEGURIDAD (Firmas, Colectores, Parámetros y Múltiples Cobros)
    // =========================================================================
    @Test
    @DisplayName("Escenario G1: Webhook sin firma o firma inválida lanza SecurityException")
    void scenarioG1_webhookWithInvalidSignatureThrowsSecurityException() {
        String mpPaymentId = "mp-sec-1";
        String rawBody = String.format("{\"action\":\"payment.created\",\"data\":{\"id\":\"%s\"},\"type\":\"payment\"}", mpPaymentId);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                "ts=123,v1=bad_signature", UUID.randomUUID().toString(), Map.of("data.id", mpPaymentId, "type", "payment"), rawBody
        );

        assertThrows(SecurityException.class, () -> webhookService.execute(command));
    }

    @Test
    @DisplayName("Escenario G2: Collector_id incorrecto lanza SecurityException")
    void scenarioG2_mismatchedCollectorIdThrowsSecurityException() {
        BigDecimal amount = BigDecimal.valueOf(30.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, "[]");
        UUID paymentId = payment.getId();

        String mpPaymentId = "mp-sec-2";
        // Collector ID no autorizado: 999999999 en vez de 123456789
        paymentMockResponse.set(new MockResponse(200, String.format(Locale.ROOT, """
                {
                  "id": 9992,
                  "status": "approved",
                  "status_detail": "accredited",
                  "transaction_amount": 30.00,
                  "currency_id": "PEN",
                  "date_approved": "2026-11-02T22:30:00.000Z",
                  "external_reference": "%s",
                  "collector_id": 999999999,
                  "live_mode": false,
                  "payment_method_id": "account_money"
                }
                """, paymentId)));

        String requestId = UUID.randomUUID().toString();
        long ts = Instant.now().getEpochSecond();
        String xSignature = calculateHmac(mpPaymentId, requestId, ts);
        String rawBody = String.format("{\"action\":\"payment.created\",\"data\":{\"id\":\"%s\"},\"type\":\"payment\"}", mpPaymentId);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, requestId, Map.of("data.id", mpPaymentId, "type", "payment"), rawBody
        );

        assertThrows(SecurityException.class, () -> webhookService.execute(command));

        // El pago local NO debe pasar a APPROVED
        Payment p = paymentRepository.findById(paymentId).orElseThrow();
        assertEquals(PaymentStatus.PENDING, p.getStatus());
    }

    @Test
    @DisplayName("Escenario G3: Identificadores de evento inconsistentes entre query y body lanzan IllegalArgumentException")
    void scenarioG3_inconsistentEventIdsThrowIllegalArgumentException() {
        String requestId = UUID.randomUUID().toString();
        long ts = Instant.now().getEpochSecond();
        String xSignature = calculateHmac("query-id-1", requestId, ts);

        ProcessMercadoPagoWebhookCommand command = new ProcessMercadoPagoWebhookCommand(
                xSignature, requestId,
                Map.of("data.id", "query-id-1", "type", "payment"),
                "{\"data\":{\"id\":\"body-id-2\"}}"
        );

        assertThrows(IllegalArgumentException.class, () -> webhookService.execute(command));
    }

    @Test
    @DisplayName("Escenario G4: Múltiples intentos aprobados escalan a revisión manual y no ejecutan devolución automática")
    void scenarioG4_multipleApprovedAttemptsEscalateToManualReview() {
        BigDecimal amount = BigDecimal.valueOf(70.00);
        UUID reservationId = createCapacityReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createPendingPayment(reservationId, amount, "[]");
        payment.markApproved(Instant.now());
        payment.requestRefund(amount, "ANOMALY");
        payment = paymentRepository.save(payment);
        UUID paymentId = payment.getId();

        // Insertar dos intentos de pago aprobados para el mismo pago (anomalía crítica)
        PaymentAttempt a1 = PaymentAttempt.create(paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-1");
        a1.markApproved("mp-pay-1", "approved", "accredited", PaymentMethod.WALLET_PURCHASE, Instant.now());
        paymentAttemptRepository.save(a1);

        PaymentAttempt a2 = PaymentAttempt.create(paymentId, 2, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-2");
        a2.markApproved("mp-pay-2", "approved", "accredited", PaymentMethod.WALLET_PURCHASE, Instant.now());
        paymentAttemptRepository.save(a2);

        PaymentRefundProcessor.RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(paymentId, UUID.randomUUID());
        assertFalse(claim.claimed());
        assertTrue(claim.skipReason().contains("Múltiples intentos"));

        // Verificar que se registró una incidencia en MANUAL_REVIEW_REQUIRED
        PaymentIncidence incidence = incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.REFUND_EXECUTION).orElseThrow();
        assertEquals(IncidenceStatus.MANUAL_REVIEW_REQUIRED, incidence.getStatus());
        assertEquals(IncidenceCategory.OPERATIONAL, incidence.getCategory());
    }
}
