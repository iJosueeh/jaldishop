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
        assertEquals(OrderReconciliationCandidateResult.Status.SUCCESS_ALREADY_APPROVED, result.status());
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

    private static class Fixture {
        final PaymentRepository payments = mock(PaymentRepository.class);
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
                    new PaymentWebhookPersistenceService(payments, attempts), null, null, tx);
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
        var processor = new PaymentOrderReconciliationProcessor(payments, attempts, null, persistence, null, null, tx);
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
