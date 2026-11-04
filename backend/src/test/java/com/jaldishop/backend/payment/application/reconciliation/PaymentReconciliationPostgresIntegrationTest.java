package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("PaymentReconciliationPostgresIntegrationTest — Pruebas de concurrencia e integración real con PostgreSQL aislado")
class PaymentReconciliationPostgresIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    @DynamicPropertySource
    static void configureIsolatedPostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.hikari.jdbc-url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.hikari.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.hikari.password", database::password);
        registry.add("spring.flyway.url", database::url);
        registry.add("spring.flyway.user", database::username);
        registry.add("spring.flyway.password", database::password);
        registry.add("app.payment.reconciliation.enabled", () -> "false");
    }

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentIncidenceRepository incidenceRepository;

    @Autowired
    private PaymentIncidenceRecorder incidenceRecorder;

    @Autowired
    private PaymentConfirmationRunner confirmationRunner;

    @Autowired
    private PaymentReconciliationProcessor reconciliationProcessor;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e3");
    private static final UUID PRODUCT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf201");
    private static final UUID VARIANT_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302");

    @BeforeEach
    void setUp() {
        assertNotNull(paymentRepository);
    }

    private UUID createTestReservation(String status, Instant expiresAt) {
        UUID reservationId = UUID.randomUUID();
        Instant protectionExpires = "PAYMENT_PROTECTED".equals(status) ? Instant.now().plusSeconds(1800) : null;
        jdbcTemplate.update(
                "INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, " +
                        "status, expires_at, payment_protection_expires_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                reservationId, STORE_ID, USER_ID, Date.valueOf(LocalDate.now().plusDays(1)),
                Time.valueOf(LocalTime.of(10, 0)), Time.valueOf(LocalTime.of(11, 0)),
                status, expiresAt != null ? java.sql.Timestamp.from(expiresAt) : java.sql.Timestamp.from(Instant.now().plusSeconds(1800)),
                protectionExpires != null ? java.sql.Timestamp.from(protectionExpires) : null,
                java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        );
        return reservationId;
    }

    private Payment createApprovedPayment(UUID reservationId, BigDecimal amount, String snapshot) {
        Payment payment = Payment.createPending(
                reservationId,
                amount,
                "PEN",
                FulfillmentType.PICKUP,
                "Cliente Test",
                "987654321",
                "cliente@test.com",
                null,
                null,
                null,
                null,
                amount,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                snapshot != null ? snapshot : "[]"
        );
        payment.markApproved(Instant.now());
        return paymentRepository.save(payment);
    }

    @Test
    @DisplayName("1. Dos workers intentando crear la misma incidencia concurrentemente no deben duplicar ni perder incrementos")
    void shouldHandleTwoWorkersCreatingSameIncidenceConcurrently() throws Exception {
        UUID reservationId = createTestReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createApprovedPayment(reservationId, new BigDecimal("100.00"), "[]");
        UUID paymentId = payment.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Void> task1 = () -> {
            latch.await();
            incidenceRecorder.recordTechnicalRetry(paymentId, new RuntimeException("Error hilo 1"));
            return null;
        };
        Callable<Void> task2 = () -> {
            latch.await();
            incidenceRecorder.recordTechnicalRetry(paymentId, new RuntimeException("Error hilo 2"));
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);
        latch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verificar que en base de datos existe EXACTAMENTE 1 fila activa
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payment_incidences WHERE payment_id = ? AND status = 'OPEN'",
                Integer.class, paymentId
        );
        assertEquals(1, count, "No deben existir incidencias activas duplicadas");

        PaymentIncidence active = incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION).orElseThrow();
        assertEquals(2, active.getAttemptsCount(), "El attempts_count debe reflejar ambos incrementos (2)");
    }

    @Test
    @DisplayName("2. Dos workers incrementando attempts_count simultáneamente deben serializarse sin lost updates")
    void shouldIncrementAttemptsCountConcurrentlyWithoutLostUpdates() throws Exception {
        UUID reservationId = createTestReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createApprovedPayment(reservationId, new BigDecimal("100.00"), "[]");
        UUID paymentId = payment.getId();

        // Crear incidencia inicial con attempt = 1
        incidenceRecorder.recordTechnicalRetry(paymentId, new RuntimeException("Intento 1 inicial"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<Void> inc1 = () -> {
            latch.await();
            incidenceRecorder.recordTechnicalRetry(paymentId, new RuntimeException("Incremento A"));
            return null;
        };
        Callable<Void> inc2 = () -> {
            latch.await();
            incidenceRecorder.recordTechnicalRetry(paymentId, new RuntimeException("Incremento B"));
            return null;
        };

        Future<Void> f1 = executor.submit(inc1);
        Future<Void> f2 = executor.submit(inc2);
        latch.countDown();

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        PaymentIncidence active = incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION).orElseThrow();
        assertEquals(3, active.getAttemptsCount(), "attempts_count debe ser exactamente 3 (1 + 2 concurrentes)");
    }

    @Test
    @DisplayName("3. Dos conciliadores procesando el mismo Payment simultáneamente no deben duplicar la orden")
    void shouldAvoidDuplicateOrdersWhenTwoReconcilersCompete() throws Exception {
        UUID reservationId = createTestReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));

        // Hold activo para que ConfirmPurchaseService no falle
        UUID inventoryReservationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 1, 'ACTIVE', NOW() + INTERVAL '30 minutes', NOW(), NOW())",
                inventoryReservationId, STORE_ID, reservationId, VARIANT_ID
        );

        // Snapshot válido completo con tracksInventory: true
        String validSnapshot = String.format("""
                [{"variantId":"%s","productId":"%s","productName":"Torta","presentationName":"Individual","sku":"TORTA-CHOC-IND","imageUrl":null,"quantity":1,"unitPriceAmount":12.50,"currency":"PEN","subtotalAmount":12.50,"tracksInventory":true}]
                """, VARIANT_ID, PRODUCT_ID);

        Payment payment = createApprovedPayment(reservationId, new BigDecimal("12.50"), validSnapshot);
        UUID paymentId = payment.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<ReconciliationCandidateResult> worker1 = () -> {
            latch.await();
            return reconciliationProcessor.processCandidate(paymentId);
        };
        Callable<ReconciliationCandidateResult> worker2 = () -> {
            latch.await();
            return reconciliationProcessor.processCandidate(paymentId);
        };

        Future<ReconciliationCandidateResult> f1 = executor.submit(worker1);
        Future<ReconciliationCandidateResult> f2 = executor.submit(worker2);
        latch.countDown();

        ReconciliationCandidateResult r1 = f1.get(10, TimeUnit.SECONDS);
        ReconciliationCandidateResult r2 = f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactamente 1 orden en base de datos
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE payment_id = ?",
                Integer.class, paymentId
        );
        assertEquals(1, orderCount, "Debe existir exactamente 1 orden persistida para el pago");

        // Al menos uno debe reportar SUCCESS
        List<ReconciliationCandidateResult.Status> statuses = List.of(r1.status(), r2.status());
        assertTrue(statuses.contains(ReconciliationCandidateResult.Status.SUCCESS), "Al menos uno debe reportar éxito");
    }

    @Test
    @DisplayName("4. Fallo comercial por reserva expirada debe hacer rollback total y persistir refundStatus=PENDING")
    void shouldRollbackOrderAndPersistCommercialFailureWhenReservationExpired() {
        // Reserva expirada en el pasado
        UUID reservationId = createTestReservation("EXPIRED", Instant.now().minusSeconds(600));

        String validSnapshot = String.format("""
                [{"variantId":"%s","productId":"%s","productName":"Torta","presentationName":"Individual","sku":"TORTA-CHOC-IND","imageUrl":null,"quantity":1,"unitPriceAmount":12.50,"currency":"PEN","subtotalAmount":12.50,"tracksInventory":true}]
                """, VARIANT_ID, PRODUCT_ID);

        Payment payment = createApprovedPayment(reservationId, new BigDecimal("12.50"), validSnapshot);
        UUID paymentId = payment.getId();

        ReconciliationCandidateResult result = reconciliationProcessor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.COMMERCIAL_FAILURE, result.status());

        // Verificar que NO se creó ninguna orden
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE payment_id = ?",
                Integer.class, paymentId
        );
        assertEquals(0, orderCount, "No debe haberse creado ninguna orden");

        // Verificar que el pago tiene refund_status = PENDING en PostgreSQL
        String refundStatus = jdbcTemplate.queryForObject(
                "SELECT refund_status FROM payments WHERE id = ?",
                String.class, paymentId
        );
        assertEquals("PENDING", refundStatus);

        // Verificar que se persistió la incidencia comercial cerrada
        Integer incidenceCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payment_incidences WHERE payment_id = ? AND category = 'COMMERCIAL' AND status = 'RESOLVED'",
                Integer.class, paymentId
        );
        assertEquals(1, incidenceCount);
    }

    @Test
    @DisplayName("5. Conciliador debe omitir pagos con reintento agendado a futuro (backoff vigente)")
    void shouldSkipPaymentWhenReattemptHasBackoffStillActive() {
        UUID reservationId = createTestReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createApprovedPayment(reservationId, new BigDecimal("45.00"), "[]");
        UUID paymentId = payment.getId();

        // Insertar incidencia con next_retry_at a futuro (+10 min)
        jdbcTemplate.update(
                "INSERT INTO payment_incidences (id, payment_id, process_type, category, code, message, attempts_count, max_attempts, next_retry_at, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'ORDER_CONFIRMATION', 'TECHNICAL', 'LOCK_TIMEOUT', 'Timeout previo', 1, 5, ?, 'OPEN', NOW(), NOW())",
                UUID.randomUUID(), paymentId, java.sql.Timestamp.from(Instant.now().plus(Duration.ofMinutes(10)))
        );

        ReconciliationCandidateResult result = reconciliationProcessor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.SKIPPED, result.status());

        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE payment_id = ?",
                Integer.class, paymentId
        );
        assertEquals(0, orderCount);
    }

    @Test
    @DisplayName("6. Conciliador debe omitir pagos en MANUAL_REVIEW_REQUIRED sin solicitar reembolsos automáticos")
    void shouldSkipPaymentWhenIncidenceIsManualReviewRequired() {
        UUID reservationId = createTestReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));
        Payment payment = createApprovedPayment(reservationId, new BigDecimal("45.00"), "[]");
        UUID paymentId = payment.getId();

        // Insertar incidencia con MANUAL_REVIEW_REQUIRED
        jdbcTemplate.update(
                "INSERT INTO payment_incidences (id, payment_id, process_type, category, code, message, attempts_count, max_attempts, next_retry_at, status, created_at, updated_at) " +
                        "VALUES (?, ?, 'ORDER_CONFIRMATION', 'TECHNICAL', 'PERSISTENT_ERROR', 'Agotó intentos', 5, 5, NULL, 'MANUAL_REVIEW_REQUIRED', NOW(), NOW())",
                UUID.randomUUID(), paymentId
        );

        ReconciliationCandidateResult result = reconciliationProcessor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.SKIPPED, result.status());

        // Pago debe permanecer APPROVED y refund_status = NOT_REQUIRED
        String refundStatus = jdbcTemplate.queryForObject(
                "SELECT refund_status FROM payments WHERE id = ?",
                String.class, paymentId
        );
        assertEquals("NOT_REQUIRED", refundStatus, "No se debe solicitar reembolso por revisión manual");
    }

    @Test
    @DisplayName("7. Confirmación exitosa no debe descontar inventario dos veces ante llamadas repetidas")
    void shouldConfirmPurchaseWithoutDoubleInventoryDeductionOnReconciliation() {
        UUID reservationId = createTestReservation("PAYMENT_PROTECTED", Instant.now().plusSeconds(1800));

        // Insertar hold de inventario para la variante
        UUID inventoryReservationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory_reservations (id, store_id, capacity_reservation_id, variant_id, quantity, status, expires_at, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 2, 'ACTIVE', NOW() + INTERVAL '30 minutes', NOW(), NOW())",
                inventoryReservationId, STORE_ID, reservationId, VARIANT_ID
        );

        Integer initialStock = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventories WHERE variant_id = ?",
                Integer.class, VARIANT_ID
        );
        assertNotNull(initialStock);

        String validSnapshot = String.format("""
                [{"variantId":"%s","productId":"%s","productName":"Torta","presentationName":"Individual","sku":"TORTA-CHOC-IND","imageUrl":null,"quantity":2,"unitPriceAmount":12.50,"currency":"PEN","subtotalAmount":25.00,"tracksInventory":true}]
                """, VARIANT_ID, PRODUCT_ID);

        Payment payment = createApprovedPayment(reservationId, new BigDecimal("25.00"), validSnapshot);
        UUID paymentId = payment.getId();

        // 1ra ejecución: Confirmación exitosa
        ReconciliationCandidateResult r1 = reconciliationProcessor.processCandidate(paymentId);
        assertEquals(ReconciliationCandidateResult.Status.SUCCESS, r1.status());

        Integer stockAfterFirst = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventories WHERE variant_id = ?",
                Integer.class, VARIANT_ID
        );
        assertEquals(initialStock - 2, stockAfterFirst, "El stock debe haberse descontado exactamente en 2 unidades");

        // 2da ejecución (reintento o conciliación duplicada): Idempotencia
        ReconciliationCandidateResult r2 = reconciliationProcessor.processCandidate(paymentId);
        assertEquals(ReconciliationCandidateResult.Status.SUCCESS, r2.status(), "Debe retornar éxito idempotente informando la orden existente");
        assertEquals(r1.orderNumber(), r2.orderNumber(), "Debe informar el mismo número de orden");

        Integer stockAfterSecond = jdbcTemplate.queryForObject(
                "SELECT quantity FROM inventories WHERE variant_id = ?",
                Integer.class, VARIANT_ID
        );
        assertEquals(stockAfterFirst, stockAfterSecond, "El stock no debe descontarse por segunda vez");
    }
}
