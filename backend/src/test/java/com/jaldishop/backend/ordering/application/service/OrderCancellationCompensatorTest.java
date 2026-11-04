package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.capacity.application.reservation.service.ReleaseCommittedCapacityReservationService;
import com.jaldishop.backend.inventory.application.service.RestockInventoryService;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCancellationCompensator — Pruebas unitarias de compensación transaccional")
class OrderCancellationCompensatorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReleaseCommittedCapacityReservationService releaseCapacityService;

    @Mock
    private RestockInventoryService restockInventoryService;

    private OrderCancellationCompensator compensator;

    private UUID customerId;
    private UUID storeId;
    private UUID paymentId;
    private UUID capacityReservationId;
    private UUID variantId;
    private String orderNumber;

    @BeforeEach
    void setUp() {
        compensator = new OrderCancellationCompensator(
                paymentRepository,
                releaseCapacityService,
                restockInventoryService
        );

        customerId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        capacityReservationId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        orderNumber = "JAL-2026-000001";
    }

    @Test
    @DisplayName("Compensación completa: cancelación desde CONFIRMED libera capacidad, repone inventario y solicita reembolso")
    void shouldCompensateCapacityInventoryAndRequestRefundWhenCancelledFromConfirmed() {
        Order order = createSampleConfirmedOrder();
        Payment payment = createSampleApprovedPayment(paymentId, new BigDecimal("55.00"));

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));

        compensator.compensate(order, OrderStatus.CONFIRMED, "Cancelación por cliente");

        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        assertThat(payment.getRefundAmount()).isEqualByComparingTo(new BigDecimal("55.00"));
        verify(paymentRepository).save(payment);

        verify(releaseCapacityService).execute(capacityReservationId);
        verify(restockInventoryService).execute(storeId, capacityReservationId);
    }

    @Test
    @DisplayName("Regla RN-ORD-13: cancelación desde IN_PREPARATION NO libera capacidad ni inventario, pero sí solicita reembolso")
    void shouldRequestRefundOnlyWithoutRestitutingCapacityOrInventoryWhenCancelledFromInPreparation() {
        Order order = createSampleConfirmedOrder();
        Payment payment = createSampleApprovedPayment(paymentId, new BigDecimal("55.00"));

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));

        compensator.compensate(order, OrderStatus.IN_PREPARATION, "Cancelación en cocina por falla técnica");

        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);
        verify(paymentRepository).save(payment);

        // Capacidad e inventario NO deben ser tocados
        verify(releaseCapacityService, never()).execute(any());
        verify(restockInventoryService, never()).execute(any(), any());
    }

    @Test
    @DisplayName("Idempotencia financiera: si el pago ya tenía reembolso PENDING o COMPLETED, no se duplica la solicitud")
    void shouldNotDuplicateRefundIfAlreadyPendingOrCompleted() {
        Order order = createSampleConfirmedOrder();
        Payment payment = createSampleApprovedPayment(paymentId, new BigDecimal("55.00"));
        payment.requestRefund(new BigDecimal("55.00"), "Reembolso previo");

        when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));

        compensator.compensate(order, OrderStatus.CONFIRMED, "Segunda solicitud");

        verify(paymentRepository, never()).save(any());
        verify(releaseCapacityService).execute(capacityReservationId);
        verify(restockInventoryService).execute(storeId, capacityReservationId);
    }

    private Order createSampleConfirmedOrder() {
        return Order.createConfirmed(
                new Order.CreateData(
                        customerId,
                        storeId,
                        paymentId,
                        capacityReservationId,
                        DeliveryMode.DELIVERY,
                        LocalDate.now().plusDays(1),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        "Cliente Ejemplo",
                        "987654321",
                        "cliente@example.com",
                        "Av. Las Palmeras 123",
                        null,
                        null,
                        null,
                        "PEN",
                        new BigDecimal("50.00"),
                        BigDecimal.ZERO,
                        null,
                        new BigDecimal("5.00"),
                        new BigDecimal("7.63"),
                        new BigDecimal("55.00"),
                        List.of(new Order.ItemData(variantId, "Torta Chocolate", "Porción", 2, new BigDecimal("25.00")))
                ),
                orderNumber
        );
    }

    private Payment createSampleApprovedPayment(UUID id, BigDecimal amount) {
        Payment payment = Payment.createPending(
                UUID.randomUUID(), amount, "PEN",
                FulfillmentType.DELIVERY, "Cliente Ejemplo", "987654321", "cliente@example.com",
                "Av. Las Palmeras 123", null, null, null,
                new BigDecimal("50.00"), BigDecimal.ZERO, null, new BigDecimal("5.00"), new BigDecimal("7.63"), "{}"
        );
        payment.markApproved(Instant.now());
        return payment;
    }
}
