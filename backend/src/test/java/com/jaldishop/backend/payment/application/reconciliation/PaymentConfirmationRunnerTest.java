package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.enums.IncidenceCategory;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentConfirmationRunner — Pruebas unitarias de ejecución transaccional")
class PaymentConfirmationRunnerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ConfirmPurchaseService confirmPurchaseService;

    @Mock
    private PaymentIncidenceRepository incidenceRepository;

    private PaymentConfirmationRunner runner;

    private UUID paymentId;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        runner = new PaymentConfirmationRunner(paymentRepository, orderRepository, confirmPurchaseService, incidenceRepository);
        paymentId = UUID.randomUUID();
        testPayment = Payment.createPending(
                UUID.randomUUID(),
                new BigDecimal("200.00"),
                "PEN",
                FulfillmentType.PICKUP,
                "Ana Perez",
                "987654321",
                "ana@example.com",
                null,
                null,
                null,
                null,
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "[]"
        );
    }

    @Test
    @DisplayName("Debe omitir la confirmación si el pago no se encuentra en estado APPROVED")
    void shouldSkipConfirmationIfPaymentIsNotApproved() {
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));

        OrderConfirmation result = runner.confirmUnderLock(paymentId);

        assertNull(result);
        verify(confirmPurchaseService, never()).execute(any());
    }

    @Test
    @DisplayName("Debe omitir la confirmación si el pago tiene refundStatus distinto de NOT_REQUIRED")
    void shouldSkipConfirmationIfPaymentHasPendingRefund() {
        testPayment.markApproved(Instant.now());
        testPayment.requestRefund(new BigDecimal("200.00"), "INCIDENCE:PREVIOUS");
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));

        OrderConfirmation result = runner.confirmUnderLock(paymentId);

        assertNull(result);
        verify(confirmPurchaseService, never()).execute(any());
    }

    @Test
    @DisplayName("Debe omitir la confirmación si el pago tiene una incidencia activa en MANUAL_REVIEW_REQUIRED")
    void shouldSkipConfirmationIfIncidenceRequiresManualReview() {
        testPayment.markApproved(Instant.now());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), paymentId, IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL, "MANUAL", "Requiere soporte", 5, 5, null,
                IncidenceStatus.MANUAL_REVIEW_REQUIRED, Instant.now(), Instant.now(), null
        );
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(incidence));

        OrderConfirmation result = runner.confirmUnderLock(paymentId);

        assertNull(result);
        verify(confirmPurchaseService, never()).execute(any());
    }

    @Test
    @DisplayName("Debe omitir la confirmación si el pago tiene un reintento programado a futuro (backoff vigente)")
    void shouldSkipConfirmationIfIncidenceRetryIsStillInTheFuture() {
        testPayment.markApproved(Instant.now());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));

        Instant futureRetry = Instant.now().plus(Duration.ofMinutes(10));
        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), paymentId, IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL, "LOCK_TIMEOUT", "Timeout", 2, 5, futureRetry,
                IncidenceStatus.OPEN, Instant.now(), Instant.now(), null
        );
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(incidence));

        OrderConfirmation result = runner.confirmUnderLock(paymentId);

        assertNull(result);
        verify(confirmPurchaseService, never()).execute(any());
    }

    @Test
    @DisplayName("Debe omitir la confirmación si la incidencia ya alcanzó el máximo de intentos")
    void shouldSkipConfirmationIfIncidenceAttemptsCountExhausted() {
        testPayment.markApproved(Instant.now());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), paymentId, IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL, "LOCK_TIMEOUT", "Timeout", 5, 5, Instant.now().minusSeconds(10),
                IncidenceStatus.OPEN, Instant.now(), Instant.now(), null
        );
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(incidence));

        OrderConfirmation result = runner.confirmUnderLock(paymentId);

        assertNull(result);
        verify(confirmPurchaseService, never()).execute(any());
    }

    @Test
    @DisplayName("Idempotencia: si ya existe una orden para el pago, debe retornar confirmación existente sin reejecutar")
    void shouldReturnExistingConfirmationIfOrderAlreadyExists() {
        testPayment.markApproved(Instant.now());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());

        Order existingOrder = mock(Order.class);
        when(existingOrder.getId()).thenReturn(UUID.randomUUID());
        when(existingOrder.getOrderNumber()).thenReturn("ORD-EXISTING-99");
        when(existingOrder.getStatus()).thenReturn(OrderStatus.CONFIRMED);
        when(existingOrder.getStoreId()).thenReturn(UUID.randomUUID());
        when(existingOrder.getCustomerName()).thenReturn("Ana Perez");
        when(existingOrder.getTotalAmount()).thenReturn(new BigDecimal("200.00"));
        when(existingOrder.getCurrency()).thenReturn("PEN");
        when(existingOrder.getConfirmedAt()).thenReturn(Instant.now());

        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(existingOrder));

        OrderConfirmation result = runner.confirmUnderLock(paymentId);

        assertNotNull(result);
        assertEquals("ORD-EXISTING-99", result.orderNumber());
        verify(confirmPurchaseService, never()).execute(any());
    }

    @Test
    @DisplayName("Debe ejecutar ConfirmPurchaseService exitosamente cuando el pago está APPROVED y sin orden")
    void shouldExecuteConfirmPurchaseServiceSuccessfully() {
        testPayment.markApproved(Instant.now());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        OrderConfirmation confirmation = new OrderConfirmation(
                UUID.randomUUID(), "ORD-2026-NEW", OrderStatus.CONFIRMED, UUID.randomUUID(),
                "Tienda Demo", "Ana Perez", new BigDecimal("200.00"), "PEN", Instant.now()
        );
        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class))).thenReturn(confirmation);

        OrderConfirmation result = runner.confirmUnderLock(paymentId);

        assertNotNull(result);
        assertEquals("ORD-2026-NEW", result.orderNumber());
        verify(confirmPurchaseService).execute(argThat(cmd -> cmd.paymentId().equals(paymentId)));
    }

    @Test
    @DisplayName("Las excepciones de conflicto deben propagarse para permitir el rollback transaccional")
    void shouldPropagateConflictExceptionForRollback() {
        testPayment.markApproved(Instant.now());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new ConflictException("RESERVATION_PROTECTION_EXPIRED", "Proteccion vencida"));

        ConflictException ex = assertThrows(ConflictException.class, () -> runner.confirmUnderLock(paymentId));
        assertEquals("RESERVATION_PROTECTION_EXPIRED", ex.getCode());
    }

    @Test
    @DisplayName("Las excepciones transitorias deben propagarse para permitir el rollback transaccional")
    void shouldPropagateTransientExceptionForRollback() {
        testPayment.markApproved(Instant.now());
        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(testPayment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        when(confirmPurchaseService.execute(any(ConfirmPurchaseCommand.class)))
                .thenThrow(new CannotAcquireLockException("Lock timeout"));

        assertThrows(CannotAcquireLockException.class, () -> runner.confirmUnderLock(paymentId));
    }
}
