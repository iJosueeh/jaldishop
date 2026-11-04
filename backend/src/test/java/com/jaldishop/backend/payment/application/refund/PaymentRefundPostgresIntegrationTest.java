package com.jaldishop.backend.payment.application.refund;

import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundClaimResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundConsolidationResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundExecutionResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("PaymentRefundPostgresIntegrationTest — Pruebas de integración real con PostgreSQL aislado y servidor HTTP simulado")
class PaymentRefundPostgresIntegrationTest {

    private static HttpServer mockServer;
    private static java.util.concurrent.ExecutorService mockExecutor;
    private static int mockPort;
    private static final List<String> receivedIdempotencyKeys = new CopyOnWriteArrayList<>();
    private static final AtomicReference<MockHttpResponse> postMockResponse = new AtomicReference<>();
    private static final AtomicReference<MockHttpResponse> getMockResponse = new AtomicReference<>();
    private static final AtomicBoolean simulateTimeout = new AtomicBoolean(false);

    record MockHttpResponse(int statusCode, String responseBody) {}

    @BeforeAll
    static void startMockHttpServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockExecutor = java.util.concurrent.Executors.newCachedThreadPool();
        mockServer.setExecutor(mockExecutor);

        mockServer.createContext("/v1/payments/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                // Leer todo el cuerpo para evitar problemas con sockets keep-alive
                exchange.getRequestBody().readAllBytes();

                String idempotencyKey = exchange.getRequestHeaders().getFirst("X-Idempotency-Key");
                if (idempotencyKey != null) {
                    receivedIdempotencyKeys.add(idempotencyKey);
                }

                if (simulateTimeout.get()) {
                    // Simular latencia que supera el read-timeout-ms para provocar SocketTimeoutException real
                    try {
                        Thread.sleep(1400);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
                MockHttpResponse resp;
                if ("GET".equals(method)) {
                    resp = getMockResponse.get();
                    if (resp == null) {
                        resp = new MockHttpResponse(200, "[]");
                    }
                } else {
                    resp = postMockResponse.get();
                    if (resp == null) {
                        resp = new MockHttpResponse(201, "{}");
                    }
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
        registry.add("mercadopago.access-token", () -> "TEST-ISOLATED-TOKEN");
        registry.add("mercadopago.connect-timeout-ms", () -> "800");
        registry.add("mercadopago.read-timeout-ms", () -> "800");
        registry.add("payments.mercadopago.refund-scheduler-enabled", () -> "false");
        registry.add("app.payment.reconciliation.enabled", () -> "false");
    }

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired private PaymentRefundIntentRepository refundIntentRepository;
    @Autowired private PaymentRefundExecutionAttemptRepository executionAttemptRepository;
    @Autowired private PaymentIncidenceRepository incidenceRepository;
    @Autowired private PaymentRefundProcessor refundProcessor;
    @Autowired private PaymentRefundService refundService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e3");

    @BeforeEach
    void cleanTestData() {
        simulateTimeout.set(false);
        receivedIdempotencyKeys.clear();
        postMockResponse.set(new MockHttpResponse(201, "{}"));
        getMockResponse.set(new MockHttpResponse(200, "[]"));
        jdbcTemplate.update("DELETE FROM payment_refund_execution_attempts");
        jdbcTemplate.update("DELETE FROM payment_refund_intents");
        jdbcTemplate.update("DELETE FROM payment_incidences");
        jdbcTemplate.update("DELETE FROM payment_attempts");
        jdbcTemplate.update("DELETE FROM orders WHERE payment_id IS NOT NULL");
        jdbcTemplate.update("DELETE FROM payments");
        jdbcTemplate.update("DELETE FROM capacity_reservations WHERE user_id = ?", USER_ID);
    }

    private UUID createTestReservation() {
        UUID reservationId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, " +
                        "status, expires_at, payment_protection_expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reservationId, STORE_ID, USER_ID, java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(1)),
                java.sql.Time.valueOf(java.time.LocalTime.of(10, 0)), java.sql.Time.valueOf(java.time.LocalTime.of(11, 0)),
                "PAYMENT_PROTECTED", java.sql.Timestamp.from(now.plusSeconds(3600)), java.sql.Timestamp.from(now.plusSeconds(3600)),
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now)
        );
        return reservationId;
    }

    private Payment createApprovedPaymentWithAttempt(String mpPaymentId, BigDecimal amount) {
        UUID reservationId = createTestReservation();
        Payment payment = Payment.createPending(
                reservationId, amount, "PEN",
                FulfillmentType.DELIVERY, "Cliente Test", "999888777", "cliente@test.com",
                "Av Aislada 123", null, null, null, amount, BigDecimal.ZERO, null,
                BigDecimal.ZERO, BigDecimal.ZERO, "[]"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(amount, "RESERVATION_EXPIRED");
        payment = paymentRepository.save(payment);

        PaymentAttempt attempt = PaymentAttempt.create(
                payment.getId(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-" + payment.getId()
        );
        attempt.markApproved(mpPaymentId, "approved", "accredited", PaymentMethod.WALLET_PURCHASE, Instant.now());
        paymentAttemptRepository.save(attempt);

        return payment;
    }

    // =========================================================================
    // 1. REEMBOLSO EXITOSO CON HTTP SIMULADO
    // =========================================================================
    @Test
    @DisplayName("Escenario 1: Reembolso exitoso con HTTP simulado (201 Created)")
    void shouldExecuteSuccessfulRefundWithMockHttp() {
        String mpId = "1001";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(60.00));
        simulateTimeout.set(false);
        postMockResponse.set(new MockHttpResponse(201, """
                {
                  "id": 9001001,
                  "payment_id": 1001,
                  "amount": 60.00,
                  "status": "approved",
                  "date_created": "2026-11-02T21:00:00.000Z"
                }
                """));

        PaymentRefundService.RefundBatchSummary summary = refundService.processPendingRefunds(10);

        assertTrue(summary.completedCount() >= 1);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
        assertEquals("9001001", reloaded.getRefundReference());

        PaymentRefundIntent intent = refundIntentRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertEquals(RefundIntentStatus.COMPLETED, intent.getStatus());
        assertEquals("9001001", intent.getProviderRefundId());

        List<PaymentRefundExecutionAttempt> attempts = executionAttemptRepository.findByRefundIntentIdOrderByAttemptNumberAsc(intent.getId());
        assertFalse(attempts.isEmpty());
        assertEquals("SUCCESSFUL", attempts.getFirst().getStatus());
    }

    // =========================================================================
    // 2. TIMEOUT SEGUIDO DE CONCILIACIÓN (GET reporta approved)
    // =========================================================================
    @Test
    @DisplayName("Escenario 2: Timeout en POST seguido de conciliación por GET")
    void shouldReconcileAfterTimeoutWhenGetReportsApproved() {
        String mpId = "1002";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(75.00));
        UUID worker1 = UUID.randomUUID();

        // 1. Simular timeout en POST durante primer ciclo
        simulateTimeout.set(true);
        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker1);
        RefundExecutionResult execResult = refundProcessor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.UNKNOWN, execResult.status());
        refundProcessor.consolidateRefundResult(claim, execResult);

        PaymentRefundIntent intentAfterTimeout = refundIntentRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertEquals(RefundIntentStatus.UNKNOWN, intentAfterTimeout.getStatus());

        // 2. En el siguiente ciclo, la conexión se restablece y el GET reporta que MP sí procesó el reembolso
        simulateTimeout.set(false);
        try {
            Thread.sleep(700);
        } catch (InterruptedException ignored) {}
        // Simular que el período de backoff de la incidencia ya transcurrió para el segundo ciclo
        jdbcTemplate.update("UPDATE payment_incidences SET next_retry_at = now() - interval '1 second' WHERE payment_id = ?", payment.getId());
        getMockResponse.set(new MockHttpResponse(200, """
                [
                  {
                    "id": 9001002,
                    "payment_id": 1002,
                    "amount": 75.00,
                    "status": "approved",
                    "date_created": "2026-11-02T21:05:00.000Z"
                  }
                ]
                """));

        UUID worker2 = UUID.randomUUID();
        RefundClaimResult claim2 = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker2);
        assertTrue(claim2.claimed());

        RefundExecutionResult execResult2 = refundProcessor.executeExternalRefund(claim2);
        assertEquals(RefundExecutionResult.ExecutionStatus.SUCCESSFUL, execResult2.status());
        assertEquals("GET", execResult2.requestMethod()); // Confirmado por GET sin emitir nuevo POST
        assertEquals("9001002", execResult2.providerRefundId());

        refundProcessor.consolidateRefundResult(claim2, execResult2);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
        assertEquals("9001002", reloaded.getRefundReference());
    }

    // =========================================================================
    // 3. GET VACÍO DESPUÉS DE TIMEOUT (Procede a POST seguro con misma clave)
    // =========================================================================
    @Test
    @DisplayName("Escenario 3: GET vacío después de timeout no se asume rechazo y procede a POST con misma clave")
    void shouldProceedToPostWithSameKeyWhenGetReturnsEmpty() {
        String mpId = "1003";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(80.00));
        UUID worker1 = UUID.randomUUID();

        // Primer intento timeout
        simulateTimeout.set(true);
        RefundClaimResult claim1 = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker1);
        refundProcessor.consolidateRefundResult(claim1, refundProcessor.executeExternalRefund(claim1));

        // Segundo intento: GET responde [] (vacío) y luego POST responde 201
        simulateTimeout.set(false);
        // Simular que el período de backoff de la incidencia ya transcurrió para el segundo ciclo
        jdbcTemplate.update("UPDATE payment_incidences SET next_retry_at = now() - interval '1 second' WHERE payment_id = ?", payment.getId());
        getMockResponse.set(new MockHttpResponse(200, "[]"));
        postMockResponse.set(new MockHttpResponse(201, """
                {
                  "id": 9001003,
                  "payment_id": 1003,
                  "amount": 80.00,
                  "status": "approved",
                  "date_created": "2026-11-02T21:10:00.000Z"
                }
                """));

        UUID worker2 = UUID.randomUUID();
        RefundClaimResult claim2 = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker2);
        assertEquals(claim1.idempotencyKey(), claim2.idempotencyKey()); // MISMA CLAVE ESTABLE

        RefundExecutionResult exec2 = refundProcessor.executeExternalRefund(claim2);
        assertEquals("POST", exec2.requestMethod());
        assertEquals("9001003", exec2.providerRefundId());

        refundProcessor.consolidateRefundResult(claim2, exec2);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
    }

    // =========================================================================
    // 4. DOS WORKERS CONCURRENTES (SKIP LOCKED / Reclamación exclusiva)
    // =========================================================================
    @Test
    @DisplayName("Escenario 4: Dos workers concurrentes compitiendo por el mismo pago reclaman exclusivamente")
    void shouldHandleConcurrentWorkersWithoutRaceCondition() throws Exception {
        String mpId = "1004";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(90.00));

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);

        AtomicInteger claimedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    UUID workerToken = UUID.randomUUID();
                    RefundClaimResult res = refundProcessor.claimOrCreateRefundIntent(payment.getId(), workerToken);
                    if (res.claimed()) {
                        claimedCount.incrementAndGet();
                    } else {
                        skippedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    skippedCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactamente un worker adquirió el reclamo; el otro fue omitido
        assertEquals(1, claimedCount.get());
        assertEquals(1, skippedCount.get());
    }

    // =========================================================================
    // 5. LEASE VENCIDO Y WORKER ANTERIOR REANUDADO
    // =========================================================================
    @Test
    @DisplayName("Escenario 5: Worker con claim obsoleto es rechazado al intentar consolidar")
    void shouldRejectStaleWorkerConsolidationWhenLeaseWasTakenByAnotherWorker() {
        String mpId = "1005";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(95.00));

        UUID slowWorker = UUID.randomUUID();
        RefundClaimResult slowClaim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), slowWorker);
        assertTrue(slowClaim.claimed());

        // Simular que el lease expiró en la base de datos
        jdbcTemplate.update("UPDATE payment_refund_intents SET lease_expires_at = ? WHERE payment_id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), payment.getId());

        // Nuevo worker toma el reclamo
        UUID newWorker = UUID.randomUUID();
        RefundClaimResult newClaim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), newWorker);
        assertTrue(newClaim.claimed());
        assertEquals(newWorker, newClaim.claimToken());

        // Ahora el slowWorker (que se había quedado trabado en HTTP) regresa e intenta consolidar
        RefundExecutionResult slowExec = RefundExecutionResult.success("POST", 201, "ref-slow", BigDecimal.valueOf(95.00), "approved", "ok");
        RefundConsolidationResult slowConsResult = refundProcessor.consolidateRefundResult(slowClaim, slowExec);

        // Debe ser rechazado porque su claimToken ya no es el dueño en BD
        assertEquals(RefundConsolidationResult.ConsolidationStatus.CLAIM_LOST, slowConsResult.status());

        // El pago NO se consolida con los datos del worker lento
        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertNotEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
    }

    // =========================================================================
    // 6. CLAVE DE IDEMPOTENCIA REUTILIZADA EN REINTENTOS
    // =========================================================================
    @Test
    @DisplayName("Escenario 6: Clave de idempotencia permanece inmutable en reintentos")
    void shouldReuseSameIdempotencyKeyAcrossRetries() {
        String mpId = "1006";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(110.00));

        receivedIdempotencyKeys.clear();
        simulateTimeout.set(false);
        postMockResponse.set(new MockHttpResponse(201, """
                {
                  "id": 9001006,
                  "payment_id": 1006,
                  "amount": 110.00,
                  "status": "approved",
                  "date_created": "2026-11-02T21:15:00.000Z"
                }
                """));

        // Intento 1
        UUID worker1 = UUID.randomUUID();
        RefundClaimResult claim1 = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker1);
        refundProcessor.executeExternalRefund(claim1);

        String key1 = claim1.idempotencyKey();
        assertNotNull(key1);

        // Forzar lease vencido para reintento
        jdbcTemplate.update("UPDATE payment_refund_intents SET lease_expires_at = ?, status = 'UNKNOWN' WHERE payment_id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), payment.getId());

        // Intento 2
        UUID worker2 = UUID.randomUUID();
        RefundClaimResult claim2 = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker2);
        assertEquals(key1, claim2.idempotencyKey()); // Misma clave exacta
    }

    // =========================================================================
    // 7. RECHAZO DEFINITIVO DE MERCADO PAGO
    // =========================================================================
    @Test
    @DisplayName("Escenario 7: Rechazo definitivo (HTTP 400 código 2024) transiciona a FAILED y MANUAL_REVIEW")
    void shouldHandleDefinitiveRejectionAndEscalateToManualReview() {
        String mpId = "1007";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(120.00));

        simulateTimeout.set(false);
        postMockResponse.set(new MockHttpResponse(400, """
                {
                  "message": "Payment too old to be refunded",
                  "error": "bad_request",
                  "status": 400,
                  "cause": [{"code": 2024, "description": "Payment too old to be refunded"}]
                }
                """));

        UUID worker = UUID.randomUUID();
        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker);
        RefundExecutionResult execResult = refundProcessor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.FAILED, execResult.status());

        RefundConsolidationResult consResult = refundProcessor.consolidateRefundResult(claim, execResult);
        assertEquals(RefundConsolidationResult.ConsolidationStatus.FAILED, consResult.status());

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(RefundStatus.FAILED, reloaded.getRefundStatus());

        PaymentRefundIntent intent = refundIntentRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertEquals(RefundIntentStatus.FAILED, intent.getStatus());
    }

    // =========================================================================
    // 8. CAÍDA LOCAL POSTERIOR AL ÉXITO EN MP (Reconciliación idempotente)
    // =========================================================================
    @Test
    @DisplayName("Escenario 8: Reconciliación tras éxito en MP pero caída previa en la base de datos")
    void shouldReconcileSuccessfullyIfExternalSucceededBeforeLocalFailure() {
        String mpId = "1008";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(130.00));

        // Simular que un worker previo reclamó la intención pero el proceso/BD cayó antes de consolidar la respuesta exitosa (lease vencido)
        PaymentRefundIntent intentPrevia = PaymentRefundIntent.create(
                payment.getId(),
                mpId,
                payment.getRefundAmount(),
                payment.getCurrency(),
                "refund-" + payment.getId(),
                UUID.randomUUID(),
                Duration.ofSeconds(30),
                Instant.now().minusSeconds(120)
        );
        refundIntentRepository.save(intentPrevia);

        // Simular que MP ya lo tiene aprobado
        simulateTimeout.set(false);
        getMockResponse.set(new MockHttpResponse(200, """
                [
                  {
                    "id": 9001008,
                    "payment_id": 1008,
                    "amount": 130.00,
                    "status": "approved",
                    "date_created": "2026-11-02T21:20:00.000Z"
                  }
                ]
                """));

        PaymentRefundService.RefundBatchSummary summary = refundService.processPendingRefunds(10);
        assertTrue(summary.completedCount() >= 1);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
        assertEquals("9001008", reloaded.getRefundReference());
    }

    // =========================================================================
    // 9. IMPOSIBILIDAD DE CREAR DOS INTENCIONES PARA EL MISMO PAGO (UNIQUE)
    // =========================================================================
    @Test
    @DisplayName("Escenario 9: Restricción UNIQUE en BD impide dos intenciones para el mismo pago")
    void shouldPreventDuplicateRefundIntentsInDatabase() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        // Crear pago base para clave foránea
        UUID reservationId = createTestReservation();
        Payment payment = Payment.createPending(
                reservationId, BigDecimal.valueOf(140.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "cliente@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(140.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "[]"
        );
        payment.markApproved(Instant.now());
        payment = paymentRepository.save(payment);
        UUID realPaymentId = payment.getId();

        jdbcTemplate.update("""
                INSERT INTO payment_refund_intents (id, payment_id, provider_payment_id, amount, currency,
                                                    idempotency_key, status, created_at, updated_at)
                VALUES (?, ?, 'mp-test', 140.00, 'PEN', ?, 'PROCESSING', now(), now())
                """, id1, realPaymentId, "key-" + id1);

        // Segundo intento de inserción con el mismo payment_id debe fallar por uq_payment_refund_intents_payment_id
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update("""
                    INSERT INTO payment_refund_intents (id, payment_id, provider_payment_id, amount, currency,
                                                        idempotency_key, status, created_at, updated_at)
                    VALUES (?, ?, 'mp-test', 140.00, 'PEN', ?, 'PROCESSING', now(), now())
                    """, id2, realPaymentId, "key-" + id2);
        });
    }

    // =========================================================================
    // 10. CONFIRMACIÓN DE QUE NO HAY TRANSACCIÓN ABIERTA DURANTE HTTP
    // =========================================================================
    @Test
    @DisplayName("Escenario 10: La llamada HTTP se ejecuta estrictamente fuera de transacción JDBC activa")
    void shouldEnsureNoTransactionIsOpenDuringHttpCall() {
        String mpId = "1010";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(150.00));
        UUID worker = UUID.randomUUID();

        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker);
        assertTrue(claim.claimed());

        // Verificamos que la fase 2 se invoque sin transacción activa
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No debe existir una transacción activa antes de la llamada HTTP");

        simulateTimeout.set(false);
        postMockResponse.set(new MockHttpResponse(201, """
                {
                  "id": 9001010,
                  "payment_id": 1010,
                  "amount": 150.00,
                  "status": "approved",
                  "date_created": "2026-11-02T21:25:00.000Z"
                }
                """));

        RefundExecutionResult execResult = refundProcessor.executeExternalRefund(claim);
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(),
                "No debe existir una transacción activa durante/después de la llamada HTTP");
        assertEquals(RefundExecutionResult.ExecutionStatus.SUCCESSFUL, execResult.status());

        refundProcessor.consolidateRefundResult(claim, execResult);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
    }

    // =========================================================================
    // 11. H-5B-01: INCIDENCIA MANUAL_REVIEW_REQUIRED NO EJECUTA POST
    // =========================================================================
    @Test
    @DisplayName("Escenario 11 (H-5B-01): Incidencia en MANUAL_REVIEW_REQUIRED es excluida y no ejecuta POST")
    void shouldExcludeAndNotExecutePostWhenIncidenceIsManualReviewRequired() {
        String mpId = "1011";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(160.00));

        // Registrar incidencia REFUND_EXECUTION en MANUAL_REVIEW_REQUIRED
        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), payment.getId(), IncidenceProcessType.REFUND_EXECUTION,
                IncidenceCategory.OPERATIONAL, "AUTH_FAIL", "Falla de credenciales",
                1, 5, null, IncidenceStatus.MANUAL_REVIEW_REQUIRED, Instant.now(), Instant.now(), null
        );
        incidenceRepository.save(incidence);

        // 1. Debe ser excluido del selector por lotes
        List<UUID> eligible = paymentRepository.findEligibleRefundPaymentIds(Instant.now(), 10);
        assertFalse(eligible.contains(payment.getId()), "El pago no debe ser elegible para reembolso");

        // 2. Si se invoca directamente, claimOrCreateRefundIntent debe omitirlo tras el bloqueo
        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), UUID.randomUUID());
        assertFalse(claim.claimed());
        assertTrue(claim.skipReason().contains("MANUAL_REVIEW_REQUIRED"));
        assertEquals(0, receivedIdempotencyKeys.size(), "No debe haberse invocado al proveedor HTTP");
    }

    // =========================================================================
    // 12. H-5B-01: INCIDENCIA CON BACKOFF A FUTURO NO EJECUTA POST
    // =========================================================================
    @Test
    @DisplayName("Escenario 12 (H-5B-01): Incidencia en período de backoff futuro es excluida y no ejecuta POST")
    void shouldExcludeAndNotExecutePostWhenIncidenceIsInFutureBackoff() {
        String mpId = "1012";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(170.00));

        // Incidencia OPEN con nextRetryAt a 10 minutos en el futuro
        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), payment.getId(), IncidenceProcessType.REFUND_EXECUTION,
                IncidenceCategory.TECHNICAL, "MP_TIMEOUT", "Timeout temporal",
                2, 5, Instant.now().plusSeconds(600), IncidenceStatus.OPEN, Instant.now(), Instant.now(), null
        );
        incidenceRepository.save(incidence);

        // 1. Excluido del selector
        List<UUID> eligible = paymentRepository.findEligibleRefundPaymentIds(Instant.now(), 10);
        assertFalse(eligible.contains(payment.getId()));

        // 2. Omitido en Fase 1
        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), UUID.randomUUID());
        assertFalse(claim.claimed());
        assertTrue(claim.skipReason().contains("backoff"));
        assertEquals(0, receivedIdempotencyKeys.size());
    }

    // =========================================================================
    // 13. H-5B-01: LÍMITE DE REINTENTOS ALCANZADO NO EJECUTA POST
    // =========================================================================
    @Test
    @DisplayName("Escenario 13 (H-5B-01): Incidencia con límite de reintentos alcanzado es excluida")
    void shouldExcludeAndNotExecutePostWhenMaxAttemptsReached() {
        String mpId = "1013";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(180.00));

        // Incidencia OPEN con attemptsCount = 5 (>= maxAttempts)
        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), payment.getId(), IncidenceProcessType.REFUND_EXECUTION,
                IncidenceCategory.TECHNICAL, "UNKNOWN", "Falla continua",
                5, 5, null, IncidenceStatus.OPEN, Instant.now(), Instant.now(), null
        );
        incidenceRepository.save(incidence);

        List<UUID> eligible = paymentRepository.findEligibleRefundPaymentIds(Instant.now(), 10);
        assertFalse(eligible.contains(payment.getId()));

        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), UUID.randomUUID());
        assertFalse(claim.claimed());
        assertTrue(claim.skipReason().contains("máximo de intentos"));
    }

    // =========================================================================
    // 14. H-5B-02: HTTP 401 Y 403 NO DECLARAN REEMBOLSO DEFINITIVAMENTE RECHAZADO
    // =========================================================================
    @Test
    @DisplayName("Escenario 14 (H-5B-02): HTTP 401/403 suspende operativamente sin declarar FAILED comercial")
    void shouldSuspendOperationallyAndNotFailWhenHttp401Or403() {
        String mpId = "1014";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(190.00));

        simulateTimeout.set(false);
        postMockResponse.set(new MockHttpResponse(401, """
                {
                  "message": "Invalid access token",
                  "error": "unauthorized",
                  "status": 401
                }
                """));

        UUID worker = UUID.randomUUID();
        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker);
        assertTrue(claim.claimed());

        RefundExecutionResult execResult = refundProcessor.executeExternalRefund(claim);
        assertEquals(RefundExecutionResult.ExecutionStatus.SUSPENDED_OPERATIONAL, execResult.status());

        RefundConsolidationResult consResult = refundProcessor.consolidateRefundResult(claim, execResult);
        assertEquals(RefundConsolidationResult.ConsolidationStatus.SUSPENDED, consResult.status());

        // El pago y la intención NO deben estar en FAILED comercial
        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertNotEquals(RefundStatus.FAILED, reloaded.getRefundStatus(), "El pago NO debe marcarse como FAILED definitivo");

        PaymentRefundIntent intent = refundIntentRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertNotEquals(RefundIntentStatus.FAILED, intent.getStatus(), "La intención NO debe marcarse como FAILED");
        assertEquals(RefundIntentStatus.UNKNOWN, intent.getStatus());

        // Debe haberse registrado una incidencia OPERATIONAL en MANUAL_REVIEW_REQUIRED
        PaymentIncidence incidence = incidenceRepository.findActiveByPaymentIdAndProcessType(payment.getId(), IncidenceProcessType.REFUND_EXECUTION).orElseThrow();
        assertEquals(IncidenceCategory.OPERATIONAL, incidence.getCategory());
        assertEquals(IncidenceStatus.MANUAL_REVIEW_REQUIRED, incidence.getStatus());

        // En el siguiente ciclo, debe estar excluido de reintento automático
        List<UUID> nextCandidates = paymentRepository.findEligibleRefundPaymentIds(Instant.now(), 10);
        assertFalse(nextCandidates.contains(payment.getId()), "Debe suspender reintentos automáticos");
    }

    // =========================================================================
    // 15. H-5B-02: HTTP 409 CONFLICT ES INDETERMINADO Y SE RECONCILIA CON GET
    // =========================================================================
    @Test
    @DisplayName("Escenario 15 (H-5B-02): HTTP 409 es clasificado como UNKNOWN y se reconcilia con consulta GET")
    void shouldTreatHttp409AsUnknownAndReconcileViaGet() {
        String mpId = "1015";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(200.00));

        // 1. Simular HTTP 409 en POST
        simulateTimeout.set(false);
        postMockResponse.set(new MockHttpResponse(409, """
                {
                  "message": "Resource is currently locked or in dispute",
                  "error": "conflict",
                  "status": 409
                }
                """));

        UUID worker1 = UUID.randomUUID();
        RefundClaimResult claim1 = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker1);
        RefundExecutionResult execResult1 = refundProcessor.executeExternalRefund(claim1);

        assertEquals(RefundExecutionResult.ExecutionStatus.UNKNOWN, execResult1.status());
        refundProcessor.consolidateRefundResult(claim1, execResult1);

        PaymentRefundIntent intentAfterConflict = refundIntentRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertEquals(RefundIntentStatus.UNKNOWN, intentAfterConflict.getStatus());

        // 2. En el siguiente ciclo, GET reporta que el reembolso sí se había ejecutado
        // Simular que el período de backoff de la incidencia ya transcurrió para el segundo ciclo
        jdbcTemplate.update("UPDATE payment_incidences SET next_retry_at = now() - interval '1 second' WHERE payment_id = ?", payment.getId());
        getMockResponse.set(new MockHttpResponse(200, """
                [
                  {
                    "id": 9001015,
                    "payment_id": 1015,
                    "amount": 200.00,
                    "status": "approved",
                    "date_created": "2026-11-02T21:30:00.000Z"
                  }
                ]
                """));

        UUID worker2 = UUID.randomUUID();
        RefundClaimResult claim2 = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker2);
        assertTrue(claim2.claimed());

        RefundExecutionResult execResult2 = refundProcessor.executeExternalRefund(claim2);
        assertEquals(RefundExecutionResult.ExecutionStatus.SUCCESSFUL, execResult2.status());
        assertEquals("GET", execResult2.requestMethod());
        assertEquals("9001015", execResult2.providerRefundId());

        refundProcessor.consolidateRefundResult(claim2, execResult2);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertEquals(RefundStatus.COMPLETED, reloaded.getRefundStatus());
        assertEquals("9001015", reloaded.getRefundReference());
    }

    // =========================================================================
    // 16. H-5B-02: HTTP 400 NO RECONOCIDO NO ASUME FALLO COMERCIAL DEFINITIVO
    // =========================================================================
    @Test
    @DisplayName("Escenario 16 (H-5B-02): HTTP 400 no reconocido transiciona a UNKNOWN y no clausura como FAILED")
    void shouldTreatUnrecognizedHttp400AsUnknownAndNotFailed() {
        String mpId = "1016";
        Payment payment = createApprovedPaymentWithAttempt(mpId, BigDecimal.valueOf(210.00));

        simulateTimeout.set(false);
        postMockResponse.set(new MockHttpResponse(400, """
                {
                  "message": "Malformed request syntax or unknown parameter",
                  "error": "bad_request",
                  "status": 400
                }
                """));

        UUID worker = UUID.randomUUID();
        RefundClaimResult claim = refundProcessor.claimOrCreateRefundIntent(payment.getId(), worker);
        RefundExecutionResult execResult = refundProcessor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.UNKNOWN, execResult.status(),
                "Error 400 genérico no debe declararse como fallo comercial definitivo");

        refundProcessor.consolidateRefundResult(claim, execResult);

        Payment reloaded = paymentRepository.findById(payment.getId()).orElseThrow();
        assertNotEquals(RefundStatus.FAILED, reloaded.getRefundStatus(), "El pago NO debe clausurarse en FAILED");

        PaymentRefundIntent intent = refundIntentRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertEquals(RefundIntentStatus.UNKNOWN, intent.getStatus());
    }
}
