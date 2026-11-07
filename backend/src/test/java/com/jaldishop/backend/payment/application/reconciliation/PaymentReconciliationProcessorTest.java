package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationProcessor — Pruebas unitarias de orquestación de fallos y rollback")
class PaymentReconciliationProcessorTest {
    @Test void suppressedIncidenceIsNeverReportedAsRecordedCommercialFailure() {
        var conflict = new ConflictException("RESERVATION_EXPIRED", "Expired");
        when(confirmationRunner.confirmUnderLock(paymentId)).thenThrow(conflict);
        when(incidenceRecorder.recordCommercialOrIndeterminateConflict(paymentId, conflict)).thenReturn(false);
        assertEquals(ReconciliationCandidateResult.Status.SKIPPED, processor.processCandidate(paymentId).status());
    }


    @Mock
    private PaymentConfirmationRunner confirmationRunner;

    @Mock
    private PaymentIncidenceRecorder incidenceRecorder;

    private PaymentReconciliationProcessor processor;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        processor = new PaymentReconciliationProcessor(confirmationRunner, incidenceRecorder);
        paymentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Debe procesar exitosamente y cerrar la incidencia")
    void shouldProcessSuccessfullyAndResolveIncidence() {
        OrderConfirmation confirmation = new OrderConfirmation(
                UUID.randomUUID(), "ORD-1234", OrderStatus.CONFIRMED, UUID.randomUUID(),
                "Tienda Demo", "Cliente", new BigDecimal("100.00"), "PEN", Instant.now()
        );
        when(confirmationRunner.confirmUnderLock(paymentId)).thenReturn(confirmation);

        ReconciliationCandidateResult result = processor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.SUCCESS, result.status());
        assertEquals("ORD-1234", result.orderNumber());
        verify(incidenceRecorder).markResolved(paymentId);
    }

    @Test
    @DisplayName("Debe registrar fallo comercial definitivo cuando runner lanza ConflictException comercial")
    void shouldHandleDefinitiveCommercialFailure() {
        ConflictException ex = new ConflictException("INSUFFICIENT_STOCK", "Stock agotado");
        when(confirmationRunner.confirmUnderLock(paymentId)).thenThrow(ex);
        when(incidenceRecorder.recordCommercialOrIndeterminateConflict(paymentId, ex)).thenReturn(true);

        ReconciliationCandidateResult result = processor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.COMMERCIAL_FAILURE, result.status());
        verify(incidenceRecorder).recordCommercialOrIndeterminateConflict(paymentId, ex);
        verify(incidenceRecorder, never()).markResolved(any());
    }

    @Test
    @DisplayName("Debe registrar conflicto indeterminado cuando runner lanza ConflictException no definitivo")
    void shouldHandleIndeterminateConflict() {
        ConflictException ex = new ConflictException("RESERVATION_NOT_PAYMENT_PROTECTED", "Reserva no protegida");
        when(confirmationRunner.confirmUnderLock(paymentId)).thenThrow(ex);
        when(incidenceRecorder.recordCommercialOrIndeterminateConflict(paymentId, ex)).thenReturn(true);

        ReconciliationCandidateResult result = processor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.INDETERMINATE_CONFLICT, result.status());
        verify(incidenceRecorder).recordCommercialOrIndeterminateConflict(paymentId, ex);
        verify(incidenceRecorder, never()).markResolved(any());
    }

    @Test
    @DisplayName("Debe registrar fallo transitorio cuando runner lanza TransientDataAccessException")
    void shouldHandleTransientDataAccessException() {
        CannotAcquireLockException ex = new CannotAcquireLockException("Lock wait timeout");
        when(confirmationRunner.confirmUnderLock(paymentId)).thenThrow(ex);

        ReconciliationCandidateResult result = processor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.TRANSIENT_FAILURE, result.status());
        verify(incidenceRecorder).recordTechnicalRetry(paymentId, ex);
        verify(incidenceRecorder, never()).markResolved(any());
    }

    @Test
    @DisplayName("Debe registrar fallo inesperado cuando runner lanza RuntimeException")
    void shouldHandleUnexpectedRuntimeException() {
        IllegalStateException ex = new IllegalStateException("Error inesperado en servicio externo");
        when(confirmationRunner.confirmUnderLock(paymentId)).thenThrow(ex);

        ReconciliationCandidateResult result = processor.processCandidate(paymentId);

        assertEquals(ReconciliationCandidateResult.Status.UNEXPECTED_FAILURE, result.status());
        verify(incidenceRecorder).recordTechnicalRetry(paymentId, ex);
        verify(incidenceRecorder, never()).markResolved(any());
    }
}
