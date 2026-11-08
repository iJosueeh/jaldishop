package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.application.webhook.PaymentWebhookPersistenceService;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderReferenceReadTest {
    @ParameterizedTest
    @CsvSource({"created,false", "created,true", "cancelled,false", "cancelled,true",
            "canceled,false", "canceled,true", "expired,false", "expired,true"})
    void emptyOrderWithValidReferencePreservesPaymentAndAppliesOnlyExpectedAttemptState(
            String status, boolean explicitReference) {
        var f = new Fixture(PaymentStatus.PENDING);
        var reference = explicitReference ? "pa1_" + f.attempt.getId() : f.paymentId.toString();
        var result = f.consolidate(f.remote("ORD1", status, reference, List.of()));
        assertEquals(status.equals("created")
                ? OrderReconciliationCandidateResult.Status.SUCCESS_ORDER_OPEN
                : OrderReconciliationCandidateResult.Status.SUCCESS_CANCELLED_OR_EXPIRED, result.status());
        assertEquals(status.equals("created") ? PaymentAttemptStatus.PROCESSING : PaymentAttemptStatus.REJECTED,
                f.attempt.getStatus());
        assertNull(f.attempt.getReconciliationClaimToken());
        assertNull(f.attempt.getProviderPaymentId());
        assertEquals(PaymentStatus.PENDING, f.payment.getStatus());
        verify(f.payments, never()).save(any());
        var locks = inOrder(f.payments, f.attempts);
        locks.verify(f.payments).findByIdForUpdate(f.paymentId);
        locks.verify(f.attempts).findByIdForUpdate(f.attempt.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"created", "cancelled", "canceled", "expired"})
    void emptyOrderWithMissingMalformedOrCrossedReferenceCannotChangeFinancialState(String status) {
        for (var paymentStatus : List.of(PaymentStatus.PENDING, PaymentStatus.APPROVED)) {
            for (String reference : Arrays.asList(null, "", "pa1_invalid", UUID.randomUUID().toString(),
                    "pa1_" + UUID.randomUUID())) {
                var f = new Fixture(paymentStatus);
                f.assertRejected(f.remote("ORD1", status, reference, List.of()));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentlyApprovedPaymentRequiresValidCorrelationBeforeReleasingLease(boolean explicitReference) {
        var f = new Fixture(PaymentStatus.APPROVED);
        var reference = explicitReference ? "pa1_" + f.attempt.getId() : f.paymentId.toString();
        var result = f.consolidate(f.remote("ORD1", "created", reference, List.of()));
        assertEquals(OrderReconciliationCandidateResult.Status.APPROVED_COMMERCIAL_PENDING, result.status());
        assertNull(f.attempt.getReconciliationClaimToken());
        assertEquals(PaymentAttemptStatus.PROCESSING, f.attempt.getStatus());
        verify(f.payments, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "APPROVED"})
    void contradictoryOrderOrMultipleTransactionsAreRejectedEvenWithValidReference(String status) {
        var f = new Fixture(PaymentStatus.valueOf(status));
        var reference = "pa1_" + f.attempt.getId();
        f.assertRejected(f.remote("OTHER_ORDER", "created", reference, List.of()));
        var rejected = new OrderQueryResult.OrderPaymentItem("11111", "rejected", null,
                BigDecimal.TEN, "PEN", null, null, null);
        var accredited = new OrderQueryResult.OrderPaymentItem("22222", "approved", "accredited",
                BigDecimal.TEN, "PEN", null, null, f.now);
        f.assertRejected(f.remote("ORD1", "processed", reference, List.of(rejected, accredited)));
    }

    @Test void siblingAttemptReferenceCannotConsolidateClaimedOrder() {
        var f = new Fixture(PaymentStatus.PENDING);
        var sibling = PaymentAttempt.create(f.paymentId, 2, PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD, "sibling", "ORDERS");
        when(f.attempts.findByPaymentIdOrderByAttemptNumberAsc(f.paymentId)).thenReturn(List.of(f.attempt, sibling));
        f.assertRejected(f.remote("ORD1", "created", "pa1_" + sibling.getId(), List.of()));
    }

    @Test void missingRemoteResponseCannotBypassCorrelationForApprovedPayment() {
        new Fixture(PaymentStatus.APPROVED).assertRejected(null);
    }

    @Test void expiredLeaseCannotBeRevivedByPassingTheOldRequestTime() {
        var f = new Fixture(PaymentStatus.PENDING);
        f.attempt.acquireReconciliationLease(f.worker, Duration.ofSeconds(1), Instant.now().minusSeconds(60));
        var result = f.processor.consolidateOrderReconciliation(f.claim,
                f.remote("ORD1", "created", f.paymentId.toString(), List.of()), f.worker, Instant.now().minusSeconds(60));
        assertEquals(OrderReconciliationCandidateResult.Status.CLAIM_LOST, result.status());
        verify(f.payments, never()).save(any());
        verify(f.attempts, never()).save(any());
    }

    @Test void cleanupReleasesExpiredOwnLeaseButNeverASuccessorsLease() {
        var f = new Fixture(PaymentStatus.PENDING);
        f.attempt.acquireReconciliationLease(f.worker, Duration.ofSeconds(1), Instant.now().minusSeconds(60));
        f.processor.releaseLeaseOnError(f.attempt.getId(), f.worker, Instant.now());
        assertNull(f.attempt.getReconciliationClaimToken());
        var successor = UUID.randomUUID();
        f.attempt.acquireReconciliationLease(successor, Duration.ofMinutes(5), Instant.now());
        f.processor.releaseLeaseOnError(f.attempt.getId(), f.worker, Instant.now());
        assertEquals(successor, f.attempt.getReconciliationClaimToken());
    }

    @Test void claimRoutesFirstThenReloadsUnderCanonicalPaymentAndAttemptLocks() {
        var f = new Fixture(PaymentStatus.PENDING);
        f.attempt.releaseReconciliationLease(Instant.now());
        when(f.attempts.findById(f.attempt.getId())).thenReturn(Optional.of(f.attempt));
        assertTrue(f.processor.claimAttempt(f.attempt.getId(), f.worker, Duration.ofMinutes(5), Instant.now()).claimed());
        var calls = inOrder(f.payments, f.attempts);
        calls.verify(f.attempts).findById(f.attempt.getId());
        calls.verify(f.payments).findByIdForUpdate(f.paymentId);
        calls.verify(f.attempts).findByIdForUpdate(f.attempt.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "in_process", "authorized", "processed"})
    void pendingTransactionIsReportedAsOpenRatherThanCancelledOrExpired(String status) {
        var f = new Fixture(PaymentStatus.PENDING);
        when(f.payment.getAmount()).thenReturn(BigDecimal.TEN);
        when(f.payment.getCurrency()).thenReturn("PEN");
        when(f.attempts.findByProviderOrderId("ORD1")).thenReturn(Optional.of(f.attempt));
        var item = new OrderQueryResult.OrderPaymentItem("remote-pending", status, null,
                BigDecimal.TEN, "PEN", null, null, null);
        var result = f.consolidate(f.remote("ORD1", "created", f.paymentId.toString(), List.of(item)));
        assertEquals(OrderReconciliationCandidateResult.Status.SUCCESS_ORDER_OPEN, result.status());
        verify(f.payment, never()).markApproved(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rejected", "cancelled"})
    void rejectedTransactionIsNotReportedAsOpen(String status) {
        var f = new Fixture(PaymentStatus.PENDING);
        when(f.payment.getAmount()).thenReturn(BigDecimal.TEN);
        when(f.payment.getCurrency()).thenReturn("PEN");
        when(f.attempts.findByProviderOrderId("ORD1")).thenReturn(Optional.of(f.attempt));
        var item = new OrderQueryResult.OrderPaymentItem("remote-rejected", status, null,
                BigDecimal.TEN, "PEN", null, null, null);

        var result = f.consolidate(f.remote("ORD1", "processed", f.paymentId.toString(), List.of(item)));

        assertEquals(OrderReconciliationCandidateResult.Status.SUCCESS_CANCELLED_OR_EXPIRED, result.status());
        assertEquals(PaymentAttemptStatus.REJECTED, f.attempt.getStatus());
        assertEquals(PaymentStatus.PENDING, f.payment.getStatus());
        assertNull(f.attempt.getReconciliationClaimToken());
        verify(f.payment, never()).markApproved(any());
        verify(f.payments, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"rejected", "cancelled"})
    void lateRejectionDoesNotOverrideAnApprovedPayment(String status) {
        var f = new Fixture(PaymentStatus.APPROVED);
        when(f.payment.getAmount()).thenReturn(BigDecimal.TEN);
        when(f.payment.getCurrency()).thenReturn("PEN");
        when(f.attempts.findByProviderOrderId("ORD1")).thenReturn(Optional.of(f.attempt));
        var item = new OrderQueryResult.OrderPaymentItem("remote-rejected", status, null,
                BigDecimal.TEN, "PEN", null, null, null);

        var result = f.consolidate(f.remote("ORD1", "processed", f.paymentId.toString(), List.of(item)));

        assertEquals(OrderReconciliationCandidateResult.Status.APPROVED_COMMERCIAL_PENDING, result.status());
        assertEquals(PaymentStatus.APPROVED, f.payment.getStatus());
        assertEquals(PaymentAttemptStatus.PROCESSING, f.attempt.getStatus());
        verify(f.payments, never()).save(any());
    }

    @Test void reconciliationUsesTheSameAccountAndEnvironmentValidatorAsWebhook() {
        var f = new Fixture(PaymentStatus.PENDING);
        f.properties.setExpectedCollectorId(123L);
        assertThrows(SecurityException.class, () -> f.consolidate(
                f.remote("ORD1", "created", f.paymentId.toString(), List.of())));
        verify(f.payments, never()).save(any());
        verify(f.attempts, never()).save(any());
    }

    @Test void approvedRemoteTransactionStillRequiresMatchingAmountAndCurrency() {
        for (boolean wrongAmount : new boolean[]{true, false}) {
            var f = new Fixture(PaymentStatus.PENDING);
            when(f.payment.getAmount()).thenReturn(BigDecimal.TEN);
            when(f.payment.getCurrency()).thenReturn("PEN");
            when(f.attempts.findByProviderOrderId("ORD1")).thenReturn(Optional.of(f.attempt));
            var item = new OrderQueryResult.OrderPaymentItem("remote-approved", "approved", "accredited",
                    wrongAmount ? BigDecimal.ONE : BigDecimal.TEN, wrongAmount ? "PEN" : "USD", null, null, null);
            assertThrows(ConflictException.class, () -> f.consolidate(
                    f.remote("ORD1", "processed", f.paymentId.toString(), List.of(item))));
            verify(f.payments, never()).save(any());
            verify(f.payment, never()).markApproved(any());
        }
    }

    @ParameterizedTest
    @CsvSource({"10.00,PEN,accredited,true", "10.00,PEN,waiting_capture,false",
            "9.00,PEN,accredited,false", "10.00,USD,accredited,false"})
    void reconciliationConsumesCanonicalOrdersResponseWithoutRelaxingFinancialChecks(
            String amount, String currency, String detail, boolean approved) throws Exception {
        var f = new Fixture(PaymentStatus.PENDING);
        when(f.payment.getAmount()).thenReturn(BigDecimal.TEN);
        when(f.payment.getCurrency()).thenReturn("PEN");
        when(f.attempts.findByProviderOrderId("ORD1")).thenReturn(Optional.of(f.attempt));
        when(f.attempts.findById(f.attempt.getId())).thenReturn(Optional.of(f.attempt));
        var status = new java.util.concurrent.atomic.AtomicReference<>(PaymentStatus.PENDING);
        when(f.payment.getStatus()).thenAnswer(inv -> status.get());
        doAnswer(inv -> { status.set(PaymentStatus.APPROVED); return null; }).when(f.payment).markApproved(any());
        f.properties.setExpectedCollectorId(1234L);
        f.properties.setExpectedLiveMode(false);
        f.properties.setAccessToken("synthetic-token");
        var mapper = new tools.jackson.databind.ObjectMapper();
        tools.jackson.databind.node.ObjectNode root;
        try (var input = getClass().getResourceAsStream("/mercadopago/orders/get-order-accredited.json")) {
            root = (tools.jackson.databind.node.ObjectNode) mapper.readTree(input);
        }
        root.put("id", "ORD1").put("external_reference", "pa1_" + f.attempt.getId())
                .put("currency", currency).put("total_amount", amount).put("total_paid_amount", amount);
        var item = (tools.jackson.databind.node.ObjectNode) root.path("transactions").path("payments").get(0);
        item.put("amount", amount).put("paid_amount", amount).put("status_detail", detail);
        var builder = org.springframework.web.client.RestClient.builder().baseUrl("https://mp.test");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://mp.test/v1/orders/ORD1"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        root.toString(), org.springframework.http.MediaType.APPLICATION_JSON));
        var adapter = new com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoRestClientAdapter(
                builder.build(), f.properties);
        var remote = adapter.queryOrder("ORD1");
        if (!amount.equals("10.00") || !currency.equals("PEN")) {
            assertThrows(ConflictException.class, () -> f.consolidate(remote));
            verify(f.payments, never()).save(any());
        } else {
            f.consolidate(remote);
            assertEquals(approved ? PaymentAttemptStatus.APPROVED : PaymentAttemptStatus.PROCESSING, f.attempt.getStatus());
            assertEquals("PAY-MAPPING", f.attempt.getProviderPaymentId());
        }
        if (approved) verify(f.payment).markApproved(any());
        else verify(f.payment, never()).markApproved(any());
        server.verify();
    }

    private static class Fixture {
        final PaymentRepository payments = mock(PaymentRepository.class);
        final com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties properties =
                new com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties();
        final PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
        final UUID paymentId = UUID.randomUUID(), worker = UUID.randomUUID();
        final Instant now = Instant.now();
        final Payment payment = mock(Payment.class);
        final PaymentAttempt attempt = PaymentAttempt.create(paymentId, 1, PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD, "key", "ORDERS");
        final PaymentOrderReconciliationProcessor processor;
        final OrderReconciliationClaimResult claim;

        Fixture(PaymentStatus status) {
            attempt.recordOrder("ORD1", "https://checkout.test");
            attempt.markProcessing();
            attempt.acquireReconciliationLease(worker, Duration.ofMinutes(5), now);
            when(payment.getId()).thenReturn(paymentId);
            when(payment.getStatus()).thenReturn(status);
            when(payments.findById(paymentId)).thenReturn(Optional.of(payment));
            when(payments.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
            when(attempts.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
            when(attempts.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(attempt));
            var tx = new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object t, TransactionDefinition d) {}
                protected void doCommit(DefaultTransactionStatus s) {}
                protected void doRollback(DefaultTransactionStatus s) {}
            };
            processor = new PaymentOrderReconciliationProcessor(payments, attempts, null,
                    new PaymentWebhookPersistenceService(payments, attempts), mock(PaymentReconciliationProcessor.class), mock(com.jaldishop.backend.ordering.domain.repository.OrderRepository.class), tx,
                    new com.jaldishop.backend.payment.application.gateway.MercadoPagoResourceValidator(
                            properties),
                    mock(PaymentIncidenceRepository.class));
            claim = OrderReconciliationClaimResult.claimed(attempt.getId(), paymentId, worker, "ORD1");
        }

        OrderQueryResult remote(String orderId, String status, String reference,
                List<OrderQueryResult.OrderPaymentItem> items) {
            return new OrderQueryResult(orderId, status, null, BigDecimal.TEN, BigDecimal.ZERO,
                    reference, items, null, null);
        }

        OrderReconciliationCandidateResult consolidate(OrderQueryResult remote) {
            return processor.consolidateOrderReconciliation(claim, remote, worker, now);
        }

        void assertRejected(OrderQueryResult remote) {
            assertThrows(ConflictException.class, () -> consolidate(remote));
            verify(payments, never()).save(any());
            verify(attempts, never()).save(any());
            verify(payment, never()).markApproved(any());
            assertEquals(PaymentAttemptStatus.PROCESSING, attempt.getStatus());
            assertNull(attempt.getProviderPaymentId());
            assertNull(attempt.getErrorCode());
            assertEquals(worker, attempt.getReconciliationClaimToken());
        }
    }

    @Test void consolidationValidatesReferenceUnderPaymentThenAttemptLocks() {
        var payments = mock(PaymentRepository.class);
        var attempts = mock(PaymentAttemptRepository.class);
        UUID paymentId = UUID.randomUUID(), worker = UUID.randomUUID();
        Instant now = Instant.now();
        var attempt = PaymentAttempt.create(paymentId, 1, PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD, "key", "ORDERS");
        attempt.recordOrder("ORD1", "https://checkout.test");
        attempt.acquireReconciliationLease(worker, Duration.ofMinutes(5), now);
        var payment = mock(Payment.class);
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(payments.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(attempts.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
        var persistence = new PaymentWebhookPersistenceService(payments, attempts);
        var tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object t, TransactionDefinition d) {}
            protected void doCommit(DefaultTransactionStatus s) {}
            protected void doRollback(DefaultTransactionStatus s) {}
        };
        var processor = new PaymentOrderReconciliationProcessor(payments, attempts, null, persistence, mock(PaymentReconciliationProcessor.class), mock(com.jaldishop.backend.ordering.domain.repository.OrderRepository.class), tx,
                    new com.jaldishop.backend.payment.application.gateway.MercadoPagoResourceValidator(
                            new com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties()),
                    mock(PaymentIncidenceRepository.class));
        var claim = OrderReconciliationClaimResult.claimed(attempt.getId(), paymentId, worker, "ORD1");
        var wrong = new OrderQueryResult("ORD1", "created", null, BigDecimal.TEN, BigDecimal.ZERO,
                "pa1_" + UUID.randomUUID(), List.of(), null, null);
        assertThrows(ConflictException.class, () -> processor.consolidateOrderReconciliation(claim, wrong, worker, now));
        verify(payments, never()).save(any()); verify(attempts, never()).save(any());
        var order = inOrder(payments, attempts);
        order.verify(payments).findByIdForUpdate(paymentId);
        order.verify(attempts).findByIdForUpdate(attempt.getId());
        var valid = new OrderQueryResult("ORD1", "created", null, BigDecimal.TEN, BigDecimal.ZERO,
                "pa1_" + attempt.getId(), List.of(), null, null);
        assertDoesNotThrow(() -> processor.consolidateOrderReconciliation(claim, valid, worker, now));
        assertEquals(PaymentAttemptStatus.CREATED, attempt.getStatus());
    }
}
