package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ManualOrderReconciliationTest {
    final PaymentRepository payments = mock(PaymentRepository.class);
    final PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
    final PaymentOrderReconciliationProcessor processor = mock(PaymentOrderReconciliationProcessor.class);
    final UUID paymentId = UUID.randomUUID();
    final Payment payment = mock(Payment.class);
    final PaymentAttempt attempt = PaymentAttempt.create(paymentId, 1, PaymentProvider.MERCADO_PAGO,
            PaymentMethod.CARD, "manual-test", "ORDERS");
    final PaymentReconciliationService service = new PaymentReconciliationService(payments, attempts,
            mock(PaymentReconciliationProcessor.class), processor, 600, 50, false, 120, 20, 300);

    ManualOrderReconciliationTest() {
        attempt.recordOrder("ORD-ISOLATED", "https://checkout.test");
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(payments.findById(paymentId)).thenReturn(Optional.of(payment));
        when(payment.getStatus()).thenReturn(PaymentStatus.PENDING);
        when(processor.claimAttempt(eq(attempt.getId()), any(), any(), any())).thenAnswer(call ->
                OrderReconciliationClaimResult.claimed(attempt.getId(), paymentId, call.getArgument(1), "ORD-ISOLATED"));
    }

    @Test void manualCallWorksWithSchedulersDisabledAndNeverSelectsBatchCandidates() {
        var remote = mock(OrderQueryResult.class);
        when(processor.queryRemoteOrder("ORD-ISOLATED")).thenReturn(remote);
        when(processor.consolidateOrderReconciliation(any(), eq(remote), any(), any())).thenReturn(
                OrderReconciliationCandidateResult.orderOpen(attempt.getId(), paymentId, "ORD-ISOLATED"));
        var result = service.reconcileOrderAttempt(attempt.getId());
        assertEquals("PENDING", result.financialStatus());
        assertEquals(OrderReconciliationCandidateResult.Status.SUCCESS_ORDER_OPEN, result.status());
        verify(attempts, never()).findEligibleOrderReconciliationAttemptIds(any(), any(), anyInt());
        verify(processor).releaseLeaseOnError(eq(attempt.getId()), any(), any());
        assertFalse(service.isOrdersReconciliationEnabled());
    }

    @Test void consolidationUsesTimeAfterRemoteCallAndCleansUpEvenOnValidationFailure() {
        var remote = mock(OrderQueryResult.class);
        Instant[] completedHttp = new Instant[1];
        when(processor.queryRemoteOrder(any())).thenAnswer(call -> { completedHttp[0] = Instant.now(); return remote; });
        when(processor.consolidateOrderReconciliation(any(), any(), any(), any())).thenAnswer(call -> {
            assertFalse(((Instant) call.getArgument(3)).isBefore(completedHttp[0]));
            throw new IllegalStateException("remote validation failed");
        });
        assertEquals(OrderReconciliationCandidateResult.Status.TECHNICAL_FAILURE,
                service.reconcileOrderAttempt(attempt.getId()).status());
        verify(processor).releaseLeaseOnError(eq(attempt.getId()), any(), any());
    }

    @Test void remoteFailurePreservesFinancialStatusAndReleasesOnlyRequestedAttempt() {
        when(processor.queryRemoteOrder(any())).thenThrow(new IllegalStateException("timeout"));
        var result = service.reconcileOrderAttempt(attempt.getId());
        assertEquals("PENDING", result.financialStatus());
        assertEquals(OrderReconciliationCandidateResult.Status.TECHNICAL_FAILURE, result.status());
        verify(processor, never()).consolidateOrderReconciliation(any(), any(), any(), any());
        verify(processor).releaseLeaseOnError(eq(attempt.getId()), any(), any());
        verify(payments, never()).save(any());
    }

    @Test void alreadyApprovedWithoutOrderIsNotCompleteSuccessAndDoesNotQueryProvider() {
        when(payment.getStatus()).thenReturn(PaymentStatus.APPROVED);
        when(processor.reconcileApprovedAttempt(attempt.getId())).thenReturn(
                OrderReconciliationCandidateResult.commercialPending(attempt.getId(), paymentId, "ORD-ISOLATED"));
        var result = service.reconcileOrderAttempt(attempt.getId());
        assertEquals("APPROVED", result.financialStatus());
        assertEquals(OrderReconciliationCandidateResult.CommercialStatus.PENDING, result.commercialStatus());
        verify(processor, never()).claimAttempt(any(), any(), any(), any());
        verify(processor, never()).queryRemoteOrder(any());
    }

    @Test void failedIncidencePersistenceNeverClaimsAnIncidenceWasRecorded() {
        when(payment.getStatus()).thenReturn(PaymentStatus.APPROVED);
        when(processor.reconcileApprovedAttempt(attempt.getId())).thenThrow(new IllegalStateException("incidence commit failed"));
        var result = service.reconcileOrderAttempt(attempt.getId());
        assertEquals("APPROVED", result.financialStatus());
        assertEquals(OrderReconciliationCandidateResult.Status.TECHNICAL_FAILURE, result.status());
        assertEquals(OrderReconciliationCandidateResult.CommercialStatus.PENDING, result.commercialStatus());
    }

    @Test void batchContinuesIfOneCandidatesLeaseCleanupFails() {
        UUID secondId = UUID.randomUUID();
        service.setOrdersReconciliationEnabled(true);
        when(attempts.findEligibleOrderReconciliationAttemptIds(any(), any(), eq(20)))
                .thenReturn(List.of(attempt.getId(), secondId));
        when(processor.reconcileApprovedAttempt(attempt.getId())).thenReturn(
                OrderReconciliationCandidateResult.orderOpen(attempt.getId(), paymentId, "ORD-ISOLATED"));
        when(processor.reconcileApprovedAttempt(secondId)).thenReturn(
                OrderReconciliationCandidateResult.orderOpen(secondId, paymentId, "ORD-SECOND"));
        doThrow(new IllegalStateException("cleanup unavailable")).when(processor)
                .releaseLeaseOnError(eq(attempt.getId()), any(), any());
        var summary = service.reconcileOrphanOrders();
        assertEquals(2, summary.totalCandidates());
        assertEquals(1, summary.failureCount());
        assertEquals(1, summary.successCount());
        verify(processor).reconcileApprovedAttempt(secondId);
    }
}
