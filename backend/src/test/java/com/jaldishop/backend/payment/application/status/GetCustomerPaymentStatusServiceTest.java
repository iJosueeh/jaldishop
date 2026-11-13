package com.jaldishop.backend.payment.application.status;

import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.model.PaymentRefundIntent;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRefundIntentRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.web.dto.CustomerPaymentStatusResponse;
import com.jaldishop.backend.shared.domain.model.CustomerSnapshot;
import com.jaldishop.backend.shared.domain.model.DeliveryAddressSnapshot;
import com.jaldishop.backend.shared.domain.model.MonetarySummary;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCustomerPaymentStatusServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private CapacityReservationRepository capacityReservationRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentIncidenceRepository paymentIncidenceRepository;

    @Mock
    private PaymentRefundIntentRepository refundIntentRepository;

    private GetCustomerPaymentStatusService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();
    private final UUID storeId = UUID.randomUUID();
    private final UUID reservationId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetCustomerPaymentStatusService(
                paymentRepository,
                capacityReservationRepository,
                orderRepository,
                paymentIncidenceRepository,
                refundIntentRepository
        );
    }

    private Payment createPaymentFixture(PaymentStatus status) {
        MonetarySummary pricing = MonetarySummary.of(
                new BigDecimal("25.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("25.00"),
                "PEN"
        );
        return Payment.rehydrate(
                paymentId,
                reservationId,
                pricing,
                status,
                RefundStatus.NOT_REQUIRED,
                null,
                null,
                status == PaymentStatus.APPROVED ? Instant.now() : null,
                null,
                Instant.now(),
                Instant.now(),
                FulfillmentType.PICKUP,
                CustomerSnapshot.of("Rosa Flores", "987654321", "rosa@example.com"),
                DeliveryAddressSnapshot.of(null, null, null, null),
                "[]"
        );
    }

    private CapacityReservation createReservationFixture(UUID ownerId) {
        CapacityReservation reservation = org.mockito.Mockito.mock(CapacityReservation.class);
        when(reservation.getUserId()).thenReturn(ownerId);
        return reservation;
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException cuando el pago no existe")
    void throwsResourceNotFoundWhenPaymentDoesNotExist() {
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(paymentId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pago no encontrado");
    }

    @Test
    @DisplayName("Lanza AccessDeniedException cuando la reserva asociada al pago no existe")
    void throwsAccessDeniedWhenReservationDoesNotExist() {
        Payment payment = createPaymentFixture(PaymentStatus.PENDING);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(paymentId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tienes autorización");
    }

    @Test
    @DisplayName("Lanza AccessDeniedException cuando el pago pertenece a otro cliente")
    void throwsAccessDeniedWhenPaymentBelongsToAnotherUser() {
        Payment payment = createPaymentFixture(PaymentStatus.PENDING);
        CapacityReservation reservation = createReservationFixture(otherUserId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> service.execute(paymentId, userId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tienes autorización");
    }

    @Test
    @DisplayName("Retorna estado PENDING cuando el pago aún no se ha confirmado ni creado orden")
    void returnsPendingStatusWhenPaymentPending() {
        Payment payment = createPaymentFixture(PaymentStatus.PENDING);
        CapacityReservation reservation = createReservationFixture(userId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(paymentIncidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());
        when(refundIntentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        CustomerPaymentStatusResponse result = service.execute(paymentId, userId);

        assertThat(result.paymentId()).isEqualTo(paymentId);
        assertThat(result.paymentStatus()).isEqualTo("PENDING");
        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderNumber()).isNull();
        assertThat(result.orderStatus()).isNull();
        assertThat(result.hasIncidence()).isFalse();
        assertThat(result.refundStatus()).isEqualTo("NOT_REQUIRED");
    }

    @Test
    @DisplayName("Retorna estado APPROVED con orderNumber cuando la compra está confirmada")
    void returnsApprovedStatusWithOrderDetails() {
        Payment payment = createPaymentFixture(PaymentStatus.APPROVED);
        CapacityReservation reservation = createReservationFixture(userId);

        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getOrderNumber()).thenReturn("ORD-998877");
        when(order.getStatus()).thenReturn(OrderStatus.CONFIRMED);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(order));
        when(paymentIncidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());
        when(refundIntentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        CustomerPaymentStatusResponse result = service.execute(paymentId, userId);

        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.orderCreated()).isTrue();
        assertThat(result.orderNumber()).isEqualTo("ORD-998877");
        assertThat(result.orderStatus()).isEqualTo("CONFIRMED");
        assertThat(result.hasIncidence()).isFalse();
    }

    @Test
    @DisplayName("Retorna orderCreated=false cuando el pago está APPROVED pero la orden aún no se ha persistido")
    void returnsOrderCreatedFalseWhenPaymentApprovedWithoutOrder() {
        Payment payment = createPaymentFixture(PaymentStatus.APPROVED);
        CapacityReservation reservation = createReservationFixture(userId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(paymentIncidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());
        when(refundIntentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        CustomerPaymentStatusResponse result = service.execute(paymentId, userId);

        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderNumber()).isNull();
        assertThat(result.orderStatus()).isNull();
        assertThat(result.hasIncidence()).isFalse();
    }

    @Test
    @DisplayName("Retorna estado FAILED cuando el pago fue rechazado")
    void returnsFailedStatusWhenPaymentRejected() {
        Payment payment = createPaymentFixture(PaymentStatus.FAILED);
        CapacityReservation reservation = createReservationFixture(userId);

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(paymentIncidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.empty());
        when(refundIntentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        CustomerPaymentStatusResponse result = service.execute(paymentId, userId);

        assertThat(result.paymentStatus()).isEqualTo("FAILED");
        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderNumber()).isNull();
        assertThat(result.hasIncidence()).isFalse();
    }

    @Test
    @DisplayName("Retorna incidencia comercial y devolución PENDING cuando la reserva expiró")
    void returnsIncidenceAndRefundPendingWhenReservationExpired() {
        Payment payment = createPaymentFixture(PaymentStatus.APPROVED);
        CapacityReservation reservation = createReservationFixture(userId);

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(),
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.COMMERCIAL,
                "PAYMENT_APPROVED_AFTER_RESERVATION_EXPIRED",
                "El pago fue aprobado después de expirar la reserva de capacidad.",
                1,
                5,
                null,
                IncidenceStatus.OPEN,
                Instant.now(),
                Instant.now(),
                null
        );

        PaymentRefundIntent refundIntent = PaymentRefundIntent.create(
                paymentId,
                "PREFERENCES",
                null,
                "mp-pay-123",
                new BigDecimal("25.00"),
                "PEN",
                "idemp-key-1",
                UUID.randomUUID(),
                Duration.ofMinutes(5),
                Instant.now()
        );

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(paymentIncidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(incidence));
        when(refundIntentRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(refundIntent));

        CustomerPaymentStatusResponse result = service.execute(paymentId, userId);

        assertThat(result.paymentStatus()).isEqualTo("APPROVED");
        assertThat(result.orderCreated()).isFalse();
        assertThat(result.orderNumber()).isNull();
        assertThat(result.hasIncidence()).isTrue();
        assertThat(result.incidenceCode()).isEqualTo("PAYMENT_APPROVED_AFTER_RESERVATION_EXPIRED");
        assertThat(result.incidenceMessage()).isEqualTo("La reserva de horario ha vencido. Se gestionará la devolución correspondiente.");
        assertThat(result.refundStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("Sanitiza mensaje de incidencia técnica para no divulgar detalles internos al cliente")
    void sanitizesTechnicalIncidenceMessageForCustomer() {
        Payment payment = createPaymentFixture(PaymentStatus.APPROVED);
        CapacityReservation reservation = createReservationFixture(userId);

        PaymentIncidence incidence = new PaymentIncidence(
                UUID.randomUUID(),
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.TECHNICAL,
                "INTERNAL_DATABASE_TIMEOUT",
                "org.postgresql.util.PSQLException: Connection refused to neon-db-internal:5432 at schema_orders",
                1,
                5,
                null,
                IncidenceStatus.OPEN,
                Instant.now(),
                Instant.now(),
                null
        );

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(capacityReservationRepository.findById(reservationId)).thenReturn(Optional.of(reservation));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(paymentIncidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION))
                .thenReturn(Optional.of(incidence));
        when(refundIntentRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());

        CustomerPaymentStatusResponse result = service.execute(paymentId, userId);

        assertThat(result.hasIncidence()).isTrue();
        assertThat(result.incidenceCode()).isEqualTo("INTERNAL_DATABASE_TIMEOUT");
        assertThat(result.incidenceMessage()).isEqualTo("Ocurrió una incidencia al procesar tu pedido. Tu pago se encuentra bajo revisión.");
        assertThat(result.incidenceMessage()).doesNotContain("org.postgresql").doesNotContain("neon-db-internal");
    }
}
