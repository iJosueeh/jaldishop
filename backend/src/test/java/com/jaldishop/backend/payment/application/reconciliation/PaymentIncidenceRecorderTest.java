package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.enums.IncidenceCategory;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentIncidenceRecorder — Pruebas unitarias de registro y serialización")
class PaymentIncidenceRecorderTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentIncidenceRepository incidenceRepository;

    private PaymentIncidenceRecorder recorder;

    private UUID paymentId;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        recorder = new PaymentIncidenceRecorder(paymentRepository, incidenceRepository);
        paymentId = UUID.randomUUID();
        testPayment = Payment.createPending(
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                "PEN",
                FulfillmentType.PICKUP,
                "Juan Perez",
                "999888777",
                "juan@example.com",
                null,
                null,
                null,
                null,
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "[]"
        );
        testPayment.markApproved(Instant.now());
    }

    @Test
    @DisplayName("Fallo comercial definitivo debe bloquear Payment, marcar refundStatus=PENDING y crear incidencia comercial cerrada")
    void shouldRecordDefinitiveCommercialFailureAndRequestRefund() {
        ConflictException ex = new ConflictException("INSUFFICIENT_STOCK", "Stock agotado para variante");
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());

        recorder.recordCommercialOrIndeterminateConflict(paymentId, ex);

        assertEquals(RefundStatus.PENDING, testPayment.getRefundStatus());
        assertTrue(testPayment.getRefundReference().contains("INSUFFICIENT_STOCK"));
        verify(paymentRepository).save(testPayment);

        ArgumentCaptor<PaymentIncidence> incidenceCaptor = ArgumentCaptor.forClass(PaymentIncidence.class);
        verify(incidenceRepository).save(incidenceCaptor.capture());
        PaymentIncidence saved = incidenceCaptor.getValue();
        assertEquals(IncidenceCategory.COMMERCIAL, saved.getCategory());
        assertEquals("INSUFFICIENT_STOCK", saved.getCode());
        assertEquals(IncidenceStatus.RESOLVED, saved.getStatus());
    }

    @Test void duplicateCommercialFailureDoesNotCreateAnotherIncidenceOrRefund() {
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        var conflict = new ConflictException("RESERVATION_EXPIRED", "Expired reservation");
        assertTrue(recorder.recordCommercialOrIndeterminateConflict(paymentId, conflict));
        when(incidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(PaymentIncidence.createCommercialTerminal(paymentId,
                        IncidenceProcessType.ORDER_CONFIRMATION, "RESERVATION_EXPIRED", "Expired", Instant.now())));
        assertTrue(recorder.recordCommercialOrIndeterminateConflict(paymentId, conflict));
        verify(paymentRepository, times(1)).save(testPayment);
        verify(incidenceRepository, times(1)).save(any(PaymentIncidence.class));
        assertEquals(RefundStatus.PENDING, testPayment.getRefundStatus());
    }

    @Test void incidenceStorageFailureMustPropagate() {
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.save(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThrows(IllegalStateException.class, () -> recorder.recordCommercialOrIndeterminateConflict(
                paymentId, new ConflictException("RESERVATION_EXPIRED", "Expired reservation")));
    }

    @Test void pendingRefundWithoutDurableCommercialEvidenceIsNotReportedAsRecorded() {
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        testPayment.requestRefund(testPayment.getAmount(), "previous-operation");
        assertFalse(recorder.recordCommercialOrIndeterminateConflict(paymentId,
                new ConflictException("RESERVATION_EXPIRED", "Expired")));
        verify(incidenceRepository, never()).save(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Conflicto indeterminado debe bloquear Payment pero NO marcar reembolso, aplicando backoff operativo")
    void shouldRecordIndeterminateConflictWithoutRequestingRefund() {
        ConflictException ex = new ConflictException("RESERVATION_NOT_PAYMENT_PROTECTED", "Reserva en estado anomalo");
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());

        recorder.recordCommercialOrIndeterminateConflict(paymentId, ex);

        // Payment fue bloqueado para serializar pero no se solicita reembolso
        verify(paymentRepository).findByIdForUpdate(paymentId);
        assertEquals(RefundStatus.NOT_REQUIRED, testPayment.getRefundStatus());

        ArgumentCaptor<PaymentIncidence> captor = ArgumentCaptor.forClass(PaymentIncidence.class);
        verify(incidenceRepository).save(captor.capture());
        PaymentIncidence saved = captor.getValue();
        assertEquals(IncidenceCategory.OPERATIONAL, saved.getCategory());
        assertEquals("RESERVATION_NOT_PAYMENT_PROTECTED", saved.getCode());
        assertEquals(IncidenceStatus.OPEN, saved.getStatus());
        assertEquals(1, saved.getAttemptsCount());
        assertNotNull(saved.getNextRetryAt());
    }

    @Test
    @DisplayName("Fallo técnico transitorio debe bloquear Payment, NO solicitar reembolso y agendar reintento")
    void shouldRecordTechnicalRetryWithoutRefund() {
        CannotAcquireLockException ex = new CannotAcquireLockException("Lock wait timeout");
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());

        recorder.recordTechnicalRetry(paymentId, ex);

        verify(paymentRepository).findByIdForUpdate(paymentId);
        assertEquals(RefundStatus.NOT_REQUIRED, testPayment.getRefundStatus());

        ArgumentCaptor<PaymentIncidence> captor = ArgumentCaptor.forClass(PaymentIncidence.class);
        verify(incidenceRepository).save(captor.capture());
        PaymentIncidence saved = captor.getValue();
        assertEquals(IncidenceCategory.TECHNICAL, saved.getCategory());
        assertEquals("CannotAcquireLockException", saved.getCode());
        assertEquals(IncidenceStatus.OPEN, saved.getStatus());
        assertEquals(1, saved.getAttemptsCount());
        assertNotNull(saved.getNextRetryAt());
    }

    @Test
    @DisplayName("Al alcanzar el límite de reintentos técnicos, debe escalar a MANUAL_REVIEW_REQUIRED")
    void shouldEscalateToManualReviewOnMaxAttempts() {
        Instant now = Instant.now();
        PaymentIncidence existing = new PaymentIncidence(
                UUID.randomUUID(),
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "LOCK_TIMEOUT",
                "Lock timeout anterior",
                4, // Ya en intento 4
                5,
                now,
                IncidenceStatus.OPEN,
                now,
                now,
                null
        );

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(existing));

        recorder.recordTechnicalRetry(paymentId, new CannotAcquireLockException("Lock timeout final"));

        verify(incidenceRepository).save(existing);
        assertEquals(5, existing.getAttemptsCount());
        assertEquals(IncidenceStatus.MANUAL_REVIEW_REQUIRED, existing.getStatus());
        assertNull(existing.getNextRetryAt());
    }

    @Test
    void integrityFailureMustAbortInsteadOfClaimingARecordedRetry() {
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.save(any())).thenThrow(new DataIntegrityViolationException("injected constraint failure"));
        assertThrows(DataIntegrityViolationException.class,
                () -> recorder.recordTechnicalRetry(paymentId, new CannotAcquireLockException("Lock error")));
    }

    @Test
    @DisplayName("Debe marcar como resuelta la incidencia activa existente cuando la orden se confirma")
    void shouldMarkActiveIncidenceResolved() {
        Instant now = Instant.now();
        PaymentIncidence existing = PaymentIncidence.createNew(
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "LOCK_TIMEOUT",
                "Timeout",
                PaymentIncidenceRecorder.calculateBackoff(1),
                now
        );

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(existing));

        recorder.markResolved(paymentId);

        verify(incidenceRepository).save(existing);
        assertEquals(IncidenceStatus.RESOLVED, existing.getStatus());
        assertNotNull(existing.getResolvedAt());
    }
}
