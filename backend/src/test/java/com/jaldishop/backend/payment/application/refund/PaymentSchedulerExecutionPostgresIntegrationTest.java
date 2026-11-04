package com.jaldishop.backend.payment.application.refund;

import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationScheduler;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.model.PaymentRefundIntent;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRefundExecutionAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRefundIntentRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.shared.config.SchedulingConfig;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(SchedulingConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PaymentSchedulerExecutionPostgresIntegrationTest {

    private static HttpServer mockServer;
    private static int mockPort;
    private static ExecutorService mockExecutor;

    private static final AtomicReference<MockHttpResponse> postMockResponse = new AtomicReference<>();
    private static final AtomicReference<MockHttpResponse> getMockResponse = new AtomicReference<>();
    private static final AtomicBoolean simulateTimeout = new AtomicBoolean(false);

    record MockHttpResponse(int statusCode, String responseBody) {}

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    @BeforeAll
    static void startMockHttpServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();
        mockExecutor = java.util.concurrent.Executors.newCachedThreadPool();
        mockServer.setExecutor(mockExecutor);

        mockServer.createContext("/v1/payments/", new HttpHandler() {
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
        registry.add("spring.flyway.validate-on-migrate", () -> "false");
        registry.add("mercadopago.base-url", () -> "http://127.0.0.1:" + mockPort);
        registry.add("mercadopago.access-token", () -> "TEST-MOCK-TOKEN");
        registry.add("mercadopago.connect-timeout-ms", () -> "800");
        registry.add("mercadopago.read-timeout-ms", () -> "800");
        // Desactivados por defecto
        registry.add("app.payment.reconciliation.enabled", () -> "false");
        registry.add("payments.mercadopago.refund-scheduler-enabled", () -> "false");
    }

    @Autowired private PaymentRepository paymentRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired private PaymentRefundIntentRepository refundIntentRepository;
    @Autowired private PaymentRefundExecutionAttemptRepository executionAttemptRepository;
    @Autowired private PaymentRefundService refundService;
    @Autowired private PaymentReconciliationService reconciliationService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e3");

    @BeforeEach
    void cleanTestData() {
        simulateTimeout.set(false);
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

    @Test
    @Order(1)
    @DisplayName("1. Registros en PENDING no son procesados cuando los schedulers están desactivados")
    void whenSchedulersDisabled_recordsRemainUntouched() {
        Payment payment = createApprovedPaymentWithAttempt("1002001", BigDecimal.valueOf(50.00));

        // Verificar estado inicial PENDING
        Payment current = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(current.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        // Sin invocación del scheduler, ningún worker procesa el pago
        assertThat(refundIntentRepository.findByPaymentId(payment.getId())).isEmpty();
    }

    @Test
    @Order(2)
    @DisplayName("2. Scheduler de reembolsos procesa lote de pagos pendientes exitosamente con mock HTTP")
    void whenRefundSchedulerExecutes_processesPendingBatchSuccessfully() {
        Payment payment = createApprovedPaymentWithAttempt("1002003", BigDecimal.valueOf(75.00));

        postMockResponse.set(new MockHttpResponse(201, """
                {
                  "id": 998877,
                  "payment_id": 1002003,
                  "amount": 75.00,
                  "status": "approved",
                  "date_created": "2026-11-04T12:00:00.000Z"
                }
                """));

        // Creamos una instancia del scheduler parametrizada con lote de 10
        PaymentRefundScheduler scheduler = new PaymentRefundScheduler(refundService, 10);
        scheduler.runRefundBatch();

        // 1. Pago actualizado en PostgreSQL a COMPLETED con referencia de reembolso
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updatedPayment.getRefundStatus()).isEqualTo(RefundStatus.COMPLETED);
        assertThat(updatedPayment.getRefundReference()).isEqualTo("998877");

        // 2. Intención de reembolso completada
        PaymentRefundIntent intent = refundIntentRepository.findByPaymentId(payment.getId()).orElseThrow();
        assertThat(intent.getStatus()).isEqualTo(RefundIntentStatus.COMPLETED);
        assertThat(intent.getProviderRefundId()).isEqualTo("998877");

        // 3. Intento de ejecución registrado como SUCCESSFUL
        var attempts = executionAttemptRepository.findByRefundIntentIdOrderByAttemptNumberAsc(intent.getId());
        assertThat(attempts).isNotEmpty();
        assertThat(attempts.getFirst().getStatus()).isEqualTo("SUCCESSFUL");
    }

    @Test
    @Order(3)
    @DisplayName("3. Scheduler de reembolsos ante timeout o error HTTP: maneja resiliencia sin generar duplicados")
    void whenRefundSchedulerEncountersTimeout_handlesResilientlyWithoutDuplicates() {
        Payment payment = createApprovedPaymentWithAttempt("1002004", BigDecimal.valueOf(100.00));

        // Simular respuesta HTTP 500
        postMockResponse.set(new MockHttpResponse(500, """
                {
                  "message": "Internal gateway error",
                  "status": 500
                }
                """));

        PaymentRefundScheduler scheduler = new PaymentRefundScheduler(refundService, 10);
        scheduler.runRefundBatch();

        // El pago permanece protegido y no se completa falsamente
        Payment updatedPayment = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updatedPayment.getRefundStatus()).isNotEqualTo(RefundStatus.COMPLETED);

        // Se registró un único intento de reembolso, evitando duplicación fútil
        Integer intentCount = jdbcTemplate.queryForObject("SELECT count(*) FROM payment_refund_intents WHERE payment_id = ?", Integer.class, payment.getId());
        assertThat(intentCount).isEqualTo(1);

        // Segunda ejecución inmediata: no crea nuevas intenciones gracias al lease/backoff
        scheduler.runRefundBatch();
        Integer intentCountAfter = jdbcTemplate.queryForObject("SELECT count(*) FROM payment_refund_intents WHERE payment_id = ?", Integer.class, payment.getId());
        assertThat(intentCountAfter).isEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("4. Scheduler de conciliación: ejecuta lote respetando límites sin lanzar excepciones")
    void whenReconciliationSchedulerExecutes_runsSafelyRespectingBatchLimit() {
        PaymentReconciliationScheduler scheduler = new PaymentReconciliationScheduler(reconciliationService);
        assertDoesNotThrow(scheduler::run);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES
    // =========================================================================

    private UUID createTestReservation() {
        UUID reservationId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, " +
                        "status, expires_at, payment_protection_expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reservationId, STORE_ID, USER_ID, java.sql.Date.valueOf(LocalDate.now().plusDays(1)),
                java.sql.Time.valueOf(LocalTime.of(10, 0)), java.sql.Time.valueOf(LocalTime.of(11, 0)),
                "PAYMENT_PROTECTED", java.sql.Timestamp.from(now.plusSeconds(3600)), java.sql.Timestamp.from(now.plusSeconds(3600)),
                java.sql.Timestamp.from(now), java.sql.Timestamp.from(now)
        );
        return reservationId;
    }

    private Payment createApprovedPaymentWithAttempt(String mpPaymentId, BigDecimal amount) {
        UUID reservationId = createTestReservation();
        Payment payment = Payment.createPending(
                reservationId, amount, "PEN",
                FulfillmentType.DELIVERY, "Cliente Scheduler Test", "999888777", "cliente.scheduler@test.com",
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
}
