package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationService — Pruebas unitarias de procesamiento por lotes")
class PaymentReconciliationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentReconciliationProcessor processor;

    private PaymentReconciliationService service;

    @BeforeEach
    void setUp() {
        // gracePeriodSeconds = 600 (10 min), batchSize = 50
        service = new PaymentReconciliationService(paymentRepository, processor, 600, 50);
    }

    @Test
    @DisplayName("Debe retornar resumen vacío si no hay pagos candidatos elegibles")
    void shouldReturnEmptySummaryWhenNoCandidatesFound() {
        when(paymentRepository.findEligibleReconciliationPaymentIds(any(Instant.class), any(Instant.class), eq(50)))
                .thenReturn(List.of());

        ReconciliationBatchSummary summary = service.reconcileOrphanApprovedPayments();

        assertNotNull(summary);
        assertEquals(0, summary.totalCandidates());
        verify(processor, never()).processCandidate(any());
    }

    @Test
    @DisplayName("Debe procesar múltiples candidatos y continuar aunque uno falle con excepción inesperada")
    void shouldProcessBatchAndContinueEvenIfSingleCandidateFailsUncaught() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        when(paymentRepository.findEligibleReconciliationPaymentIds(any(Instant.class), any(Instant.class), eq(50)))
                .thenReturn(List.of(id1, id2, id3));

        when(processor.processCandidate(id1))
                .thenReturn(ReconciliationCandidateResult.success(id1, "ORD-001"));
        when(processor.processCandidate(id2))
                .thenThrow(new RuntimeException("Error grave de infraestructura"));
        when(processor.processCandidate(id3))
                .thenReturn(ReconciliationCandidateResult.conflict(id3, "INSUFFICIENT_STOCK", "Sin stock"));

        ReconciliationBatchSummary summary = service.reconcileOrphanApprovedPayments();

        assertEquals(3, summary.totalCandidates());
        assertEquals(1, summary.successCount());
        assertEquals(1, summary.commercialFailuresCount());
        assertEquals(1, summary.technicalFailuresCount());

        verify(processor).processCandidate(id1);
        verify(processor).processCandidate(id2);
        verify(processor).processCandidate(id3);
    }
}
