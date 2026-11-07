package com.jaldishop.backend.payment.application.refund;

import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.ExecuteRefundCommand;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundQueryResult;
import com.jaldishop.backend.payment.application.gateway.dto.RefundResult;
import com.jaldishop.backend.payment.application.reconciliation.PaymentIncidenceRecorder;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundClaimResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundConsolidationResult;
import com.jaldishop.backend.payment.application.refund.PaymentRefundProcessor.RefundExecutionResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRefundProcessor — Pruebas unitarias de orquestación en 3 fases")
class PaymentRefundProcessorTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAttemptRepository paymentAttemptRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRefundIntentRepository refundIntentRepository;
    @Mock private PaymentRefundExecutionAttemptRepository executionAttemptRepository;
    @Mock private PaymentIncidenceRepository incidenceRepository;
    @Mock private MercadoPagoGateway mercadoPagoGateway;

    private PaymentRefundProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PaymentRefundProcessor(
                paymentRepository,
                paymentAttemptRepository,
                orderRepository,
                refundIntentRepository,
                executionAttemptRepository,
                incidenceRepository,
                mercadoPagoGateway
        );
    }

    @Test
    @DisplayName("Fase 1: Debe reclamar pago elegible y crear intención de reembolso")
    void shouldClaimAndCreateRefundIntent() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(50.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(50.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(50.00), "RESERVATION_EXPIRED");

        PaymentAttempt attempt = PaymentAttempt.create(
                paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-1"
        );
        attempt.markApproved("mp-pay-12345", "approved", "accredited", PaymentMethod.WALLET_PURCHASE, Instant.now());

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(attempt));
        when(refundIntentRepository.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.empty());
        when(refundIntentRepository.save(any(PaymentRefundIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundClaimResult claim = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertTrue(claim.claimed());
        assertEquals(paymentId, claim.paymentId());
        assertEquals("mp-pay-12345", claim.providerPaymentId());
        assertEquals(BigDecimal.valueOf(50.00), claim.amount());
        assertEquals("refund-" + paymentId, claim.idempotencyKey());
        assertEquals(workerToken, claim.claimToken());
        assertEquals(RefundStatus.PROCESSING, payment.getRefundStatus());
    }

    @Test
    @DisplayName("Fase 2: Reconciliación previa vía GET antes de POST ante estado UNKNOWN")
    void shouldReconcileViaGetBeforePostWhenPreviousStatusIsUnknown() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId, UUID.randomUUID(), UUID.randomUUID(), "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.UNKNOWN
        );

        when(mercadoPagoGateway.queryPaymentRefunds("mp-pay-12345")).thenReturn(List.of(
                new RefundQueryResult("ref-mp-777", "mp-pay-12345", BigDecimal.valueOf(50.00), "approved", Instant.now())
        ));

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.SUCCESSFUL, execResult.status());
        assertEquals("ref-mp-777", execResult.providerRefundId());
        assertEquals("GET", execResult.requestMethod());
        verify(mercadoPagoGateway, never()).executeRefund(any());
    }

    @Test
    @DisplayName("Fase 2: Ejecuta POST seguro cuando GET reporta array vacío")
    void shouldExecutePostWhenGetReportsEmpty() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId, UUID.randomUUID(), UUID.randomUUID(), "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.UNKNOWN
        );

        when(mercadoPagoGateway.queryPaymentRefunds("mp-pay-12345")).thenReturn(List.of());
        when(mercadoPagoGateway.executeRefund(any())).thenReturn(
                new RefundResult("ref-mp-888", "mp-pay-12345", BigDecimal.valueOf(50.00), "approved", Instant.now())
        );

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.SUCCESSFUL, execResult.status());
        assertEquals("ref-mp-888", execResult.providerRefundId());
        assertEquals("POST", execResult.requestMethod());
        verify(mercadoPagoGateway, times(1)).executeRefund(any());
    }

    @Test
    @DisplayName("Fase 3: Rechaza consolidación si el claim_token del worker expiró o fue reemplazado")
    void shouldRejectConsolidationIfClaimTokenLost() {
        UUID paymentId = UUID.randomUUID();
        UUID worker1Token = UUID.randomUUID();
        UUID worker2Token = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(50.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(50.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());

        // La intención en la base de datos ahora pertenece a worker2
        PaymentRefundIntent intentInDb = PaymentRefundIntent.create(
                paymentId, "mp-pay-12345", BigDecimal.valueOf(50.00), "PEN",
                "refund-" + paymentId, worker2Token, PaymentRefundProcessor.LEASE_DURATION, Instant.now()
        );

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(refundIntentRepository.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.of(intentInDb));

        RefundClaimResult claimWorker1 = RefundClaimResult.claimed(
                paymentId, intentInDb.getId(), worker1Token, "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.PROCESSING
        );
        RefundExecutionResult execResult = RefundExecutionResult.success("POST", 201, "ref-1", BigDecimal.valueOf(50.00), "approved", "ok");

        RefundConsolidationResult consResult = processor.consolidateRefundResult(claimWorker1, execResult);

        assertEquals(RefundConsolidationResult.ConsolidationStatus.CLAIM_LOST, consResult.status());
        verify(paymentRepository, never()).save(any());
        verify(refundIntentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fase 3: Consolida exitosamente cuando el claim_token es válido")
    void shouldConsolidateSuccessfullyWhenClaimIsValid() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(50.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(50.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(50.00), "RESERVATION_EXPIRED");

        PaymentRefundIntent intentInDb = PaymentRefundIntent.create(
                paymentId, "mp-pay-12345", BigDecimal.valueOf(50.00), "PEN",
                "refund-" + paymentId, workerToken, PaymentRefundProcessor.LEASE_DURATION, Instant.now()
        );

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(refundIntentRepository.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.of(intentInDb));
        when(executionAttemptRepository.findNextAttemptNumber(intentInDb.getId())).thenReturn(1);

        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId, intentInDb.getId(), workerToken, "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.PROCESSING
        );
        RefundExecutionResult execResult = RefundExecutionResult.success("POST", 201, "ref-mp-999", BigDecimal.valueOf(50.00), "approved", "ok");

        RefundConsolidationResult consResult = processor.consolidateRefundResult(claim, execResult);

        assertEquals(RefundConsolidationResult.ConsolidationStatus.COMPLETED, consResult.status());
        assertEquals("ref-mp-999", consResult.providerRefundId());
        assertEquals(RefundStatus.COMPLETED, payment.getRefundStatus());
        assertEquals("ref-mp-999", payment.getRefundReference());
        assertEquals(RefundIntentStatus.COMPLETED, intentInDb.getStatus());
        verify(incidenceRepository, times(1)).findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, IncidenceProcessType.REFUND_EXECUTION);
    }

    @Test
    @DisplayName("Fase 1: Omite reclamo si la incidencia de reembolso requiere revisión manual")
    void shouldSkipClaimWhenIncidenceIsInManualReviewRequired() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(50.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(50.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(50.00), "RESERVATION_EXPIRED");

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), paymentId, IncidenceProcessType.REFUND_EXECUTION,
                IncidenceCategory.OPERATIONAL, "AUTH_ERROR", "Error de token", 1, 5, null,
                IncidenceStatus.MANUAL_REVIEW_REQUIRED, Instant.now(), Instant.now(), null
        );

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.REFUND_EXECUTION))
                .thenReturn(Optional.of(incidence));

        RefundClaimResult claim = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertFalse(claim.claimed());
        assertTrue(claim.skipReason().contains("MANUAL_REVIEW_REQUIRED"));
        verify(refundIntentRepository, never()).findByPaymentIdForUpdate(any());
    }

    @Test
    @DisplayName("Fase 1: Omite reclamo si la incidencia tiene período de backoff a futuro")
    void shouldSkipClaimWhenIncidenceHasFutureBackoff() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(50.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(50.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(50.00), "RESERVATION_EXPIRED");

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), paymentId, IncidenceProcessType.REFUND_EXECUTION,
                IncidenceCategory.TECHNICAL, "MP_TIMEOUT", "Timeout temporal", 1, 5,
                Instant.now().plusSeconds(300), IncidenceStatus.OPEN, Instant.now(), Instant.now(), null
        );

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.REFUND_EXECUTION))
                .thenReturn(Optional.of(incidence));

        RefundClaimResult claim = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertFalse(claim.claimed());
        assertTrue(claim.skipReason().contains("backoff"));
        verify(refundIntentRepository, never()).findByPaymentIdForUpdate(any());
    }

    @Test
    @DisplayName("Fase 1: Omite reclamo si la incidencia superó el máximo de intentos")
    void shouldSkipClaimWhenIncidenceReachedMaxAttempts() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(50.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(50.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(50.00), "RESERVATION_EXPIRED");

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(), paymentId, IncidenceProcessType.REFUND_EXECUTION,
                IncidenceCategory.TECHNICAL, "UNKNOWN", "Error continuo", 5, 5,
                null, IncidenceStatus.OPEN, Instant.now(), Instant.now(), null
        );

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(incidenceRepository.findActiveByPaymentIdAndProcessType(paymentId, IncidenceProcessType.REFUND_EXECUTION))
                .thenReturn(Optional.of(incidence));

        RefundClaimResult claim = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertFalse(claim.claimed());
        assertTrue(claim.skipReason().contains("máximo de intentos"));
        verify(refundIntentRepository, never()).findByPaymentIdForUpdate(any());
    }

    @Test
    @DisplayName("Fase 2: Suspende operativamente ante HTTP 401 sin declarar rechazo definitivo")
    void shouldSuspendOperationallyAndNotFailWhenMercadoPagoReturns401() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId, UUID.randomUUID(), UUID.randomUUID(), "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.PENDING
        );

        when(mercadoPagoGateway.executeRefund(any())).thenThrow(
                new MercadoPagoException(401, "MP_UNAUTHORIZED", "Token inválido")
        );

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.SUSPENDED_OPERATIONAL, execResult.status());
        assertEquals(401, execResult.httpStatus());
        assertEquals("MP_UNAUTHORIZED", execResult.errorCode());
    }

    @Test
    @DisplayName("Fase 2: Clasifica HTTP 409 como indeterminado (UNKNOWN)")
    void shouldTreatHttp409AsIndeterminateUnknown() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId, UUID.randomUUID(), UUID.randomUUID(), "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.PENDING
        );

        when(mercadoPagoGateway.executeRefund(any())).thenThrow(
                new MercadoPagoException(409, "MP_CONFLICT", "Operación concurrente en disputa")
        );

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.UNKNOWN, execResult.status());
        assertEquals(409, execResult.httpStatus());
    }

    @Test
    @DisplayName("Fase 2: Clasifica HTTP 400 no reconocido como indeterminado (UNKNOWN)")
    void shouldTreatUnrecognizedHttp400AsIndeterminateUnknown() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId, UUID.randomUUID(), UUID.randomUUID(), "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.PENDING
        );

        when(mercadoPagoGateway.executeRefund(any())).thenThrow(
                new MercadoPagoException(400, "MP_BAD_REQUEST", "invalid_parameter: metadata is malformed")
        );

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.UNKNOWN, execResult.status());
    }

    @Test
    @DisplayName("Fase 2: Clasifica HTTP 400 reconocido como rechazo comercial definitivo (FAILED)")
    void shouldTreatRecognizedHttp400AsDefinitiveCommercialFailure() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId, UUID.randomUUID(), UUID.randomUUID(), "mp-pay-12345",
                BigDecimal.valueOf(50.00), "refund-" + paymentId, RefundIntentStatus.PENDING
        );

        when(mercadoPagoGateway.executeRefund(any())).thenThrow(
                new MercadoPagoException(400, "refund_amount_exceeded", "El monto a devolver excede lo disponible")
        );

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        assertEquals(RefundExecutionResult.ExecutionStatus.FAILED, execResult.status());
        assertEquals("refund_amount_exceeded", execResult.errorCode());
    }

    @Test
    @DisplayName("Fase 1: Admite reembolso cuando el pedido asociado está legítimamente CANCELLED")
    void shouldAllowRefundWhenOrderIsCancelled() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(80.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(80.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(80.00), "Cancelación de pedido");

        PaymentAttempt attempt = PaymentAttempt.create(
                paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-cancel"
        );
        attempt.markApproved("mp-pay-cancel", "approved", "accredited", PaymentMethod.CARD, Instant.now());

        com.jaldishop.backend.ordering.domain.model.Order cancelledOrder = mock(com.jaldishop.backend.ordering.domain.model.Order.class);
        when(cancelledOrder.getStatus()).thenReturn(com.jaldishop.backend.ordering.domain.enums.OrderStatus.CANCELLED);
        when(cancelledOrder.getOrderNumber()).thenReturn("JAL-2026-000001");

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(cancelledOrder));
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(attempt));
        when(refundIntentRepository.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.empty());
        when(refundIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefundClaimResult result = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertTrue(result.claimed());
        assertEquals(paymentId, result.paymentId());
        assertEquals("mp-pay-cancel", result.providerPaymentId());
    }

    @Test
    @DisplayName("Fase 1: Rechaza reembolso cuando el pedido asociado está activo en CONFIRMED")
    void shouldRejectRefundWhenOrderIsActiveConfirmed() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(80.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(80.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(80.00), "Falso intento de reembolso");

        com.jaldishop.backend.ordering.domain.model.Order activeOrder = mock(com.jaldishop.backend.ordering.domain.model.Order.class);
        when(activeOrder.getStatus()).thenReturn(com.jaldishop.backend.ordering.domain.enums.OrderStatus.CONFIRMED);
        when(activeOrder.getOrderNumber()).thenReturn("JAL-2026-000002");

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(activeOrder));

        RefundClaimResult result = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertFalse(result.claimed());
        assertEquals("El pago ya tiene pedido confirmado activo asociado", result.skipReason());
    }

    @Test
    @DisplayName("Fase 1: Rechaza reembolso cuando el pedido asociado está activo en IN_PREPARATION")
    void shouldRejectRefundWhenOrderIsActiveInPreparation() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(80.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(80.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(80.00), "Falso intento de reembolso");

        com.jaldishop.backend.ordering.domain.model.Order activeOrder = mock(com.jaldishop.backend.ordering.domain.model.Order.class);
        when(activeOrder.getStatus()).thenReturn(com.jaldishop.backend.ordering.domain.enums.OrderStatus.IN_PREPARATION);
        when(activeOrder.getOrderNumber()).thenReturn("JAL-2026-000003");

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(activeOrder));

        RefundClaimResult result = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertFalse(result.claimed());
        assertEquals("El pago ya tiene pedido confirmado activo asociado", result.skipReason());
    }

    @Test
    @DisplayName("Fase 1: Debe reclamar pago con intento en modo ORDERS y capturar providerOrderId")
    void shouldClaimAndCreateRefundIntentForOrdersMode() {
        UUID paymentId = UUID.randomUUID();
        UUID workerToken = UUID.randomUUID();

        Payment payment = Payment.createPending(
                UUID.randomUUID(), BigDecimal.valueOf(150.00), "PEN",
                FulfillmentType.DELIVERY, "Cliente", "999", "c@test.com",
                "Av Test", null, null, null, BigDecimal.valueOf(150.00),
                BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, "{}"
        );
        payment.markApproved(Instant.now());
        payment.requestRefund(BigDecimal.valueOf(150.00), "RESERVATION_EXPIRED");

        PaymentAttempt attempt = PaymentAttempt.create(
                paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.WALLET_PURCHASE, "idemp-orders-01", "ORDERS"
        );
        attempt.recordOrder("ORD01J67CQQH5904WDBVZEM4JMEP3", "https://checkout.url");
        attempt.markApproved("mp-pay-99999", "approved", "accredited", PaymentMethod.WALLET_PURCHASE, Instant.now());

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)).thenReturn(List.of(attempt));
        when(refundIntentRepository.findByPaymentIdForUpdate(paymentId)).thenReturn(Optional.empty());
        when(refundIntentRepository.save(any(PaymentRefundIntent.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundClaimResult claim = processor.claimOrCreateRefundIntent(paymentId, workerToken);

        assertTrue(claim.claimed());
        assertEquals(paymentId, claim.paymentId());
        assertEquals("ORDERS", claim.checkoutMode());
        assertEquals("ORD01J67CQQH5904WDBVZEM4JMEP3", claim.providerOrderId());
        assertEquals("mp-pay-99999", claim.providerPaymentId());
        assertEquals(BigDecimal.valueOf(150.00), claim.amount());
        assertEquals(RefundStatus.PROCESSING, payment.getRefundStatus());
    }

    @Test
    @DisplayName("Fase 2: Reconciliación previa vía GET order antes de POST en modo ORDERS ante estado UNKNOWN")
    void shouldReconcileViaGetOrderBeforePostWhenPreviousStatusIsUnknownForOrdersMode() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORDERS",
                "ORD01J67CQQH5904WDBVZEM4JMEP3",
                "mp-pay-99999",
                BigDecimal.valueOf(150.00),
                "refund-" + paymentId,
                RefundIntentStatus.UNKNOWN
        );

        OrderQueryResult orderResult = new OrderQueryResult(
                "ORD01J67CQQH5904WDBVZEM4JMEP3",
                "processed",
                "refunded",
                BigDecimal.valueOf(150.00),
                BigDecimal.valueOf(150.00),
                "ext-order",
                List.of(),
                List.of(new OrderQueryResult.OrderRefundItem(
                        "ref-order-99", "mp-pay-99999", BigDecimal.valueOf(150.00), "approved", Instant.now()
                )),
                123456L,
                false
        );
        when(mercadoPagoGateway.queryOrder("ORD01J67CQQH5904WDBVZEM4JMEP3")).thenReturn(orderResult);

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        verify(mercadoPagoGateway, times(1)).queryOrder("ORD01J67CQQH5904WDBVZEM4JMEP3");
        verify(mercadoPagoGateway, never()).executeRefund(any());

        assertEquals(RefundExecutionResult.ExecutionStatus.SUCCESSFUL, execResult.status());
        assertEquals("ref-order-99", execResult.providerRefundId());
        assertEquals("GET", execResult.requestMethod());
    }

    @Test
    @DisplayName("Fase 2: Ejecuta reembolso en modo ORDERS enviando providerOrderId")
    void shouldExecuteRefundPassingOrdersModeAndOrderId() {
        UUID paymentId = UUID.randomUUID();
        RefundClaimResult claim = RefundClaimResult.claimed(
                paymentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORDERS",
                "ORD01J67CQQH5904WDBVZEM4JMEP3",
                "mp-pay-99999",
                BigDecimal.valueOf(150.00),
                "refund-" + paymentId,
                RefundIntentStatus.PENDING
        );

        when(mercadoPagoGateway.executeRefund(argThat(cmd ->
                "ORDERS".equals(cmd.checkoutMode()) &&
                "ORD01J67CQQH5904WDBVZEM4JMEP3".equals(cmd.providerOrderId()) &&
                BigDecimal.valueOf(150.00).compareTo(cmd.amount()) == 0
        ))).thenReturn(new RefundResult("ref-mp-new-1", "mp-pay-99999", BigDecimal.valueOf(150.00), "approved", Instant.now()));

        RefundExecutionResult execResult = processor.executeExternalRefund(claim);

        verify(mercadoPagoGateway, times(1)).executeRefund(any());
        assertEquals(RefundExecutionResult.ExecutionStatus.SUCCESSFUL, execResult.status());
        assertEquals("ref-mp-new-1", execResult.providerRefundId());
        assertEquals("POST", execResult.requestMethod());
    }
}
