package com.jaldishop.backend.payment.application.refund;

import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundClaimResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundConsolidationResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundExecutionResult;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.domain.enums.RefundIntentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRefundService — Pruebas unitarias de procesamiento de lotes")
class PaymentRefundServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentRefundProcessor refundProcessor;

    @InjectMocks
    private PaymentRefundService refundService;

    @Test
    @DisplayName("Debe procesar lote de reembolsos coordinando las 3 fases")
    void shouldProcessPendingRefundsBatch() {
        UUID paymentId1 = UUID.randomUUID();
        UUID paymentId2 = UUID.randomUUID();

        when(paymentRepository.findEligibleRefundPaymentIds(any(java.time.Instant.class), eq(20))).thenReturn(List.of(paymentId1, paymentId2));

        RefundClaimResult claim1 = RefundClaimResult.claimed(
                paymentId1, UUID.randomUUID(), UUID.randomUUID(), "mp-1",
                BigDecimal.valueOf(30.00), "refund-" + paymentId1, RefundIntentStatus.PROCESSING
        );
        RefundClaimResult claim2 = RefundClaimResult.skipped("Lease activo de otro worker");

        when(refundProcessor.claimOrCreateRefundIntent(eq(paymentId1), any(UUID.class))).thenReturn(claim1);
        when(refundProcessor.claimOrCreateRefundIntent(eq(paymentId2), any(UUID.class))).thenReturn(claim2);

        RefundExecutionResult execResult1 = RefundExecutionResult.success("POST", 201, "ref-1", BigDecimal.valueOf(30.00), "approved", "ok");
        when(refundProcessor.executeExternalRefund(claim1)).thenReturn(execResult1);

        RefundConsolidationResult consResult1 = RefundConsolidationResult.completed("ref-1");
        when(refundProcessor.consolidateRefundResult(claim1, execResult1)).thenReturn(consResult1);

        PaymentRefundService.RefundBatchSummary summary = refundService.processPendingRefunds(20);

        assertEquals(2, summary.totalProcessed());
        assertEquals(1, summary.completedCount());
        assertEquals(1, summary.skippedCount());
        assertEquals(0, summary.failedCount());
        assertEquals(0, summary.indeterminateCount());

        verify(refundProcessor, times(1)).executeExternalRefund(claim1);
        verify(refundProcessor, times(1)).consolidateRefundResult(claim1, execResult1);
        verify(refundProcessor, never()).executeExternalRefund(claim2);
    }
}
