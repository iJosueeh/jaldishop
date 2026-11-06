package com.jaldishop.backend.payment.application.investigation;

import com.jaldishop.backend.BackendApplication;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.PaymentQueryResult;
import com.jaldishop.backend.payment.application.webhook.PaymentWebhookPersistenceService;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoRestClientAdapter;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jaldishop.backend.payment.application.investigation.ManualOrderInvestigationService.Outcome.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Real PostgreSQL/JPA/JDBC and transactional service proxies; HTTP never leaves the mock request factory. */
@SpringBootTest(classes = {BackendApplication.class, ManualOrderInvestigationPostgresIntegrationTest.HttpConfiguration.class},
        properties = {
                "app.security.jwt.secret=manual-investigation-test-secret-at-least-32-characters",
                "mercadopago.base-url=https://mp.invalid",
                "mercadopago.access-token=TEST-MANUAL",
                "mercadopago.expected-collector-id=123456789",
                "mercadopago.expected-live-mode=false",
                "app.payment.reconciliation.enabled=false",
                "app.payment.reconciliation.orders-enabled=false",
                "payments.mercadopago.refund-scheduler-enabled=false",
                "spring.datasource.hikari.connection-init-sql=SET lock_timeout = '5s'"
        })
class ManualOrderInvestigationPostgresIntegrationTest {
    private static final TestPostgresConfig DATABASE = TestPostgresConfig.fromEnvironment();
    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e3");
    private static final BigDecimal AMOUNT = new BigDecimal("10.00");
    private static final String CHECKOUT_URL = "https://checkout.test/manual";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::url);
        registry.add("spring.datasource.hikari.jdbc-url", DATABASE::url);
        registry.add("spring.datasource.username", DATABASE::username);
        registry.add("spring.datasource.hikari.username", DATABASE::username);
        registry.add("spring.datasource.password", DATABASE::password);
        registry.add("spring.datasource.hikari.password", DATABASE::password);
        registry.add("spring.flyway.url", DATABASE::url);
        registry.add("spring.flyway.user", DATABASE::username);
        registry.add("spring.flyway.password", DATABASE::password);
        registry.add("spring.flyway.validate-on-migrate", DATABASE::validateOnMigrate);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HttpConfiguration {
        @Bean TestHttp testHttp(MercadoPagoProperties properties) { return new TestHttp(properties); }
        @Bean @Primary MercadoPagoGateway manualTestGateway(TestHttp http) { return http.gateway; }
    }

    static class TestHttp {
        final MockRestServiceServer server;
        final MercadoPagoGateway gateway;
        TestHttp(MercadoPagoProperties properties) {
            var builder = RestClient.builder().baseUrl("https://mp.invalid");
            server = MockRestServiceServer.bindTo(builder).build();
            gateway = new MercadoPagoRestClientAdapter(builder.build(), properties);
        }
    }

    @Autowired ManualOrderInvestigationService service;
    @Autowired PaymentWebhookPersistenceService webhook;
    @Autowired PaymentRepository payments;
    @Autowired PaymentAttemptRepository attempts;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired DataSource dataSource;
    @Autowired TestHttp http;

    private UUID reservationId;
    private Payment payment;
    private PaymentAttempt attempt;
    private String orderId;
    private String providerPaymentId;
    private ExecutorService workers;
    private TransactionTemplate transaction;
    private final AtomicInteger getCount = new AtomicInteger();

    @BeforeEach
    void createIsolatedFixture() {
        http.server.reset();
        workers = Executors.newFixedThreadPool(2);
        transaction = new TransactionTemplate(manager);
        reservationId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO capacity_reservations
                (id, store_id, user_id, service_date, start_time, end_time, status, expires_at, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_DATE + 1, '10:00', '11:00', 'ACTIVE', NOW() + INTERVAL '30 minutes', NOW(), NOW())
                """, reservationId, STORE_ID, USER_ID);
        payment = payments.save(Payment.createPending(reservationId, AMOUNT, "PEN", FulfillmentType.DELIVERY,
                "Manual Test", "999999999", "manual@example.test", "Test address", null, null, null,
                AMOUNT, BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "[]"));
        attempt = PaymentAttempt.create(payment.getId(), 1, PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD, "manual-" + UUID.randomUUID(), "ORDERS");
        attempt.recordOrderCreationFailure(false);
        attempt.markProcessing();
        attempt = attempts.save(attempt);
        orderId = "ORD-MANUAL-" + attempt.getId();
        providerPaymentId = "PAY-MANUAL-" + attempt.getId();
        // Historical UNKNOWN with a known ID, without confirmed checkout evidence.
        jdbc.update("UPDATE payment_attempts SET provider_order_id = ? WHERE id = ?", orderId, attempt.getId());
        jdbc.update("""
                INSERT INTO checkout_creation_requests
                (payment_attempt_id, checkout_mode, idempotency_key, format_version, request_body)
                VALUES (?, 'ORDERS', ?, 1, ?)
                """, attempt.getId(), attempt.getIdempotencyKey(),
                "{\"external_reference\":\"" + payment.getId() + "\",\"total_amount\":\"10.00\"}");
    }

    @AfterEach
    void removeOnlyOwnFixture() throws InterruptedException {
        workers.shutdownNow();
        assertTrue(workers.awaitTermination(10, TimeUnit.SECONDS), "Workers must stop before deleting their fixture");
        try {
            http.server.verify();
        } finally {
            // V24/V25 dependent rows cascade; no global DELETE or disabling audit guards.
            if (attempt != null) jdbc.update("DELETE FROM payment_attempts WHERE id = ?", attempt.getId());
            if (payment != null) jdbc.update("DELETE FROM payments WHERE id = ?", payment.getId());
            if (reservationId != null) jdbc.update("DELETE FROM capacity_reservations WHERE id = ?", reservationId);
        }
    }

    @Test
    void verifiesHistoricalReferenceAndCommitsAssociationWithBothAudits() {
        expectValidGet(1, () -> {});

        assertEquals(VERIFIED, service.investigate(attempt.getId()).outcome());

        assertCreated();
        assertEquals(PaymentAttemptStatus.PROCESSING, reloadAttempt().getStatus());
        assertEquals(PaymentStatus.PENDING, reloadPayment().getStatus());
        assertEquals(1, events("MANUAL_IDENTITY_VERIFIED"));
        assertEquals(1, events("VALIDATED_CREATION_RESPONSE"));
        assertEquals(1, getCount.get());
    }

    @Test
    void jpaFlushFailureRollsBackPreviouslyInsertedJdbcAudit() {
        expectValidGet(1, () -> {});
        int before = totalEvents();
        String constraint = "manual_fail_" + attempt.getId().toString().replace("-", "");
        jdbc.execute("ALTER TABLE payment_attempts ADD CONSTRAINT " + constraint
                + " CHECK (id <> '" + attempt.getId() + "'::uuid OR remote_order_creation_state <> 'CREATED')");
        try {
            assertEquals(TECHNICAL_ERROR, service.investigate(attempt.getId()).outcome());
            assertUnknownWithNoCheckout();
            assertEquals(before, totalEvents());
        } finally {
            jdbc.execute("ALTER TABLE payment_attempts DROP CONSTRAINT " + constraint);
        }
    }

    @Test
    void deferredJdbcAuditFailureRollsBackFlushedJpaAssociation() {
        expectValidGet(1, () -> {});
        int before = totalEvents();
        String fault = "manual_fail_" + attempt.getId().toString().replace("-", "");
        // The deferred audit failure occurs at commit, after Hibernate has flushed the attempt UPDATE.
        jdbc.execute("CREATE FUNCTION " + fault + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
                + "RAISE EXCEPTION 'injected manual audit commit failure'; END; $$");
        try {
            jdbc.execute("CREATE CONSTRAINT TRIGGER " + fault + " AFTER INSERT ON remote_order_creation_events "
                    + "DEFERRABLE INITIALLY DEFERRED FOR EACH ROW WHEN (NEW.payment_attempt_id = '" + attempt.getId()
                    + "'::uuid AND NEW.evidence_code = 'MANUAL_IDENTITY_VERIFIED') EXECUTE FUNCTION " + fault + "()");
            try {
                assertEquals(TECHNICAL_ERROR, service.investigate(attempt.getId()).outcome());
                assertUnknownWithNoCheckout();
                assertEquals(before, totalEvents());
            } finally {
                jdbc.execute("DROP TRIGGER " + fault + " ON remote_order_creation_events");
            }
        } finally {
            jdbc.execute("DROP FUNCTION " + fault + "()");
        }
    }

    @Test
    void twoInvestigationsSerializeAndCreateOnlyOneAssociation() throws Exception {
        var bothFetched = new CyclicBarrier(2);
        expectValidGet(2, () -> cross(bothFetched));

        Future<ManualOrderInvestigationService.Result> first = workers.submit(() -> service.investigate(attempt.getId()));
        Future<ManualOrderInvestigationService.Result> second = workers.submit(() -> service.investigate(attempt.getId()));
        var results = java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));

        assertThat(results).extracting(ManualOrderInvestigationService.Result::outcome)
                .containsExactlyInAnyOrder(VERIFIED, INCONCLUSIVE);
        assertThat(results).extracting(ManualOrderInvestigationService.Result::reason)
                .containsExactlyInAnyOrder("IDENTITY_VERIFIED", "CONCURRENT_CREATION_EVIDENCE");
        assertCreated();
        assertEquals(1, events("VALIDATED_CREATION_RESPONSE"));
        assertEquals(1, events("MANUAL_IDENTITY_VERIFIED"));
        assertEquals(1, events("MANUAL_CONCURRENT_CREATION_EVIDENCE"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM payment_attempts WHERE provider_order_id = ?", Integer.class, orderId));
        assertEquals(PaymentStatus.PENDING, reloadPayment().getStatus());
        assertEquals(PaymentAttemptStatus.PROCESSING, reloadAttempt().getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"approved", "rejected"})
    void webhookHoldingPaymentCanUpdateAttemptWhileInvestigationWaits(String financialStatus) throws Exception {
        expectValidGet(1, () -> {});
        var paymentLocked = new CountDownLatch(1);
        var finishWebhook = new CountDownLatch(1);
        var webhookPid = new AtomicInteger();
        Future<?> webhookWork = workers.submit(() -> transaction.executeWithoutResult(status -> {
            payments.findByIdForUpdate(payment.getId()).orElseThrow();
            webhookPid.set(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
            paymentLocked.countDown();
            waitFor(finishWebhook);
            // Regression probe: the waiting investigation must not already own the attempt lock.
            jdbc.queryForObject("SELECT id FROM payment_attempts WHERE id = ? FOR UPDATE NOWAIT", UUID.class, attempt.getId());
            webhook.applyPaymentQueryResult(payment.getId(), webhookResult(financialStatus), orderId);
        }));
        try {
            assertTrue(paymentLocked.await(10, TimeUnit.SECONDS));
            Future<ManualOrderInvestigationService.Result> investigation = workers.submit(() -> service.investigate(attempt.getId()));
            await().atMost(Duration.ofSeconds(4)).until(() -> hasBlockedTransaction(webhookPid.get()));
            finishWebhook.countDown();

            webhookWork.get(15, TimeUnit.SECONDS);
            assertEquals(VERIFIED, investigation.get(15, TimeUnit.SECONDS).outcome());
            assertCreated();
            PaymentAttempt current = reloadAttempt();
            assertEquals("approved".equals(financialStatus) ? PaymentAttemptStatus.APPROVED : PaymentAttemptStatus.REJECTED,
                    current.getStatus());
            assertEquals("approved".equals(financialStatus) ? PaymentStatus.APPROVED : PaymentStatus.PENDING,
                    reloadPayment().getStatus());
            assertEquals(providerPaymentId, current.getProviderPaymentId());
            assertEquals(financialStatus, current.getProviderStatus());
            assertNotNull(current.getCompletedAt());
            if ("rejected".equals(financialStatus)) assertEquals("rejected", current.getErrorCode());
            assertEquals(1, events("VALIDATED_CREATION_RESPONSE"));
        } finally {
            finishWebhook.countDown();
        }
    }

    @Test
    void webhookApprovalAndCompletedRefundDuringGetArePreserved() throws Exception {
        var fetching = new CountDownLatch(1);
        var releaseGet = new CountDownLatch(1);
        expectValidGet(1, () -> { fetching.countDown(); waitFor(releaseGet); });
        Future<ManualOrderInvestigationService.Result> investigation = workers.submit(() -> service.investigate(attempt.getId()));
        Instant refundedAt = Instant.parse("2026-11-05T12:00:00Z");
        try {
            assertTrue(fetching.await(10, TimeUnit.SECONDS));
            webhook.applyPaymentQueryResult(payment.getId(), webhookResult("approved"), orderId);
            transaction.executeWithoutResult(status -> {
                Payment current = payments.findByIdForUpdate(payment.getId()).orElseThrow();
                current.requestRefund(AMOUNT, "manual-test-refund");
                current.completeRefund("manual-test-refund", refundedAt);
                payments.save(current);
            });
            webhook.applyPaymentQueryResult(payment.getId(), webhookResult("pending"), orderId);
            webhook.recordApprovalIncidence(payment.getId(), "late commercial failure");
            releaseGet.countDown();
            assertEquals(VERIFIED, investigation.get(15, TimeUnit.SECONDS).outcome());
            assertCreated();
            Payment current = reloadPayment();
            assertEquals(PaymentStatus.APPROVED, current.getStatus());
            assertEquals(RefundStatus.COMPLETED, current.getRefundStatus());
            assertThat(current.getRefundAmount()).isEqualByComparingTo(AMOUNT);
            assertEquals("manual-test-refund", current.getRefundReference());
            assertEquals(refundedAt, current.getRefundedAt());
            assertEquals(PaymentAttemptStatus.APPROVED, reloadAttempt().getStatus());
            assertEquals(providerPaymentId, reloadAttempt().getProviderPaymentId());
        } finally {
            releaseGet.countDown();
        }
    }

    @Test
    void duplicateAndLateWebhookPreserveFinancialEvidence() {
        webhook.applyPaymentQueryResult(payment.getId(), webhookResult("approved"), orderId);
        Instant completed = reloadAttempt().getCompletedAt();
        webhook.applyPaymentQueryResult(payment.getId(), webhookResult("approved"), orderId);
        webhook.applyPaymentQueryResult(payment.getId(), webhookResult("pending"), orderId);
        assertEquals(PaymentAttemptStatus.APPROVED, reloadAttempt().getStatus());
        assertEquals(completed, reloadAttempt().getCompletedAt());
    }

    @Test
    void contradictoryWebhookCommitsAuditWithoutFinancialMutation() {
        webhook.applyPaymentQueryResult(payment.getId(), webhookResult("pending"), orderId);
        Payment paymentBeforeConflict = reloadPayment();
        PaymentAttempt attemptBeforeConflict = reloadAttempt();
        assertEquals(PaymentStatus.PROCESSING, paymentBeforeConflict.getStatus());
        assertEquals(PaymentAttemptStatus.PROCESSING, attemptBeforeConflict.getStatus());
        assertEquals(0, events("WEBHOOK_IDENTIFIER_CONTRADICTION"));

        assertThrows(com.jaldishop.backend.shared.exception.ConflictException.class,
                () -> webhook.applyPaymentQueryResult(payment.getId(), webhookResult("approved"), "another-order"));
        assertThat(reloadPayment()).usingRecursiveComparison().isEqualTo(paymentBeforeConflict);
        assertThat(reloadAttempt()).usingRecursiveComparison().isEqualTo(attemptBeforeConflict);
        assertEquals(1, events("WEBHOOK_IDENTIFIER_CONTRADICTION"));
    }

    @Test
    void remoteGetSuspendsCallerTransactionAndRestoresItAfterwards() {
        expectValidGet(1, () -> {});
        transaction.executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class);
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertEquals(VERIFIED, service.investigate(attempt.getId()).outcome());
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            assertTrue(TransactionSynchronizationManager.hasResource(dataSource));
        });
        assertCreated();
        assertEquals(1, getCount.get());
    }

    @Test
    void noKnownIdRemainsUnknownWithoutAnyHttpRequestOrNewAttempt() {
        jdbc.update("UPDATE payment_attempts SET provider_order_id = NULL WHERE id = ?", attempt.getId());
        assertEquals(new ManualOrderInvestigationService.Result(INCONCLUSIVE, "NO_UNAMBIGUOUS_ORDER_ID"),
                service.investigate(attempt.getId()));
        assertUnknownWithNoCheckout();
        assertNull(reloadAttempt().getProviderOrderId());
        assertEquals(1, attempts.findByPaymentIdOrderByAttemptNumberAsc(payment.getId()).size());
        assertEquals(1, events("MANUAL_NO_UNAMBIGUOUS_ORDER_ID"));
        assertEquals(0, getCount.get());
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 429, 500})
    void remoteErrorsPreserveUnknownWithoutRetryingCreation(int status) {
        http.server.expect(requestTo("https://mp.invalid/v1/orders/" + orderId))
                .andExpect(method(HttpMethod.GET)).andExpect(request -> assertOutsideTransaction())
                .andRespond(withStatus(HttpStatus.valueOf(status)));
        String reason = status == 404 ? "REMOTE_NOT_FOUND" : "REMOTE_FAILURE";
        assertEquals(new ManualOrderInvestigationService.Result(status == 404 ? INCONCLUSIVE : TECHNICAL_ERROR, reason),
                service.investigate(attempt.getId()));
        assertUnknownWithNoCheckout();
        assertEquals(orderId, reloadAttempt().getProviderOrderId());
        assertEquals(1, attempts.findByPaymentIdOrderByAttemptNumberAsc(payment.getId()).size());
        assertEquals(1, events("MANUAL_" + reason));
    }

    private void expectValidGet(int count, Runnable duringGet) {
        String body = """
                {"id":"%s","status":"processed","external_reference":"%s","total_amount":"10.00",
                 "checkout_url":"%s","collector_id":123456789,"live_mode":false,
                 "transactions":{"payments":[{"id":"%s","status":"approved","status_detail":"accredited",
                  "transaction_amount":10.00,"currency_id":"PEN"}]}}
                """.formatted(orderId, payment.getId(), CHECKOUT_URL, providerPaymentId);
        http.server.expect(ExpectedCount.times(count), requestTo("https://mp.invalid/v1/orders/" + orderId))
                .andExpect(method(HttpMethod.GET)).andRespond(request -> {
                    assertOutsideTransaction();
                    getCount.incrementAndGet();
                    duringGet.run();
                    return withSuccess(body, MediaType.APPLICATION_JSON).createResponse(request);
                });
    }

    private void assertOutsideTransaction() {
        assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "GET must run outside a transaction");
        assertFalse(TransactionSynchronizationManager.hasResource(dataSource), "GET must have no bound JDBC connection");
    }

    private PaymentQueryResult webhookResult(String status) {
        return new PaymentQueryResult(providerPaymentId, status, "approved".equals(status) ? "accredited" : "rejected",
                AMOUNT, "PEN", "visa", "credit_card", payment.getId().toString(),
                Instant.parse("2026-11-05T11:00:00Z"), 123456789L, false);
    }

    private boolean hasBlockedTransaction(int blockerPid) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid)))
                """, Boolean.class, blockerPid));
    }

    private void assertCreated() {
        PaymentAttempt current = reloadAttempt();
        assertEquals(RemoteOrderCreationState.CREATED, current.getRemoteOrderCreationState());
        assertEquals(orderId, current.getProviderOrderId());
        assertEquals(CHECKOUT_URL, current.getInitPoint());
        assertEquals(payment.getId(), current.getPaymentId());
    }

    private void assertUnknownWithNoCheckout() {
        assertEquals(RemoteOrderCreationState.UNKNOWN, reloadAttempt().getRemoteOrderCreationState());
        assertNull(reloadAttempt().getInitPoint());
        assertEquals(PaymentAttemptStatus.PROCESSING, reloadAttempt().getStatus());
        assertEquals(PaymentStatus.PENDING, reloadPayment().getStatus());
    }

    private PaymentAttempt reloadAttempt() { return attempts.findById(attempt.getId()).orElseThrow(); }
    private Payment reloadPayment() { return payments.findById(payment.getId()).orElseThrow(); }
    private int events(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM remote_order_creation_events WHERE payment_attempt_id = ? AND evidence_code = ?",
                Integer.class, attempt.getId(), code);
    }
    private int totalEvents() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM remote_order_creation_events WHERE payment_attempt_id = ?",
                Integer.class, attempt.getId());
    }

    private static void waitFor(CountDownLatch latch) {
        try {
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent operation did not reach the expected point");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
    }

    private static void cross(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        } catch (BrokenBarrierException | TimeoutException ex) {
            throw new AssertionError(ex);
        }
    }
}
