package com.jaldishop.backend.payment.application.status;

import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.enums.RefundIntentStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.model.PaymentRefundIntent;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRefundIntentRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.web.dto.CustomerPaymentStatusResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class GetCustomerPaymentStatusService {

    private final PaymentRepository paymentRepository;
    private final CapacityReservationRepository capacityReservationRepository;
    private final OrderRepository orderRepository;
    private final PaymentIncidenceRepository paymentIncidenceRepository;
    private final PaymentRefundIntentRepository refundIntentRepository;

    public GetCustomerPaymentStatusService(
            PaymentRepository paymentRepository,
            CapacityReservationRepository capacityReservationRepository,
            OrderRepository orderRepository,
            PaymentIncidenceRepository paymentIncidenceRepository,
            PaymentRefundIntentRepository refundIntentRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.capacityReservationRepository = capacityReservationRepository;
        this.orderRepository = orderRepository;
        this.paymentIncidenceRepository = paymentIncidenceRepository;
        this.refundIntentRepository = refundIntentRepository;
    }

    @Transactional(readOnly = true)
    public CustomerPaymentStatusResponse execute(UUID paymentId, UUID authenticatedUserId) {
        Objects.requireNonNull(paymentId, "paymentId no puede ser nulo");
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId no puede ser nulo");

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + paymentId));

        // Validación de pertenencia: el pago debe corresponder a una reserva del usuario autenticado
        CapacityReservation reservation = capacityReservationRepository.findById(payment.getCapacityReservationId())
                .orElse(null);

        if (reservation == null || !reservation.getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("No tienes autorización para consultar este pago.");
        }

        // Consultar orden asociada
        Optional<Order> orderOpt = orderRepository.findByPaymentId(paymentId);
        boolean orderCreated = orderOpt.isPresent();
        String orderNumber = orderCreated ? orderOpt.get().getOrderNumber() : null;
        String orderStatus = orderCreated ? orderOpt.get().getStatus().name() : null;

        // Consultar incidencia activa
        Optional<PaymentIncidence> incidenceOpt = paymentIncidenceRepository
                .findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION);

        boolean hasIncidence = incidenceOpt.isPresent() &&
                (incidenceOpt.get().getStatus() == IncidenceStatus.OPEN
                        || incidenceOpt.get().getStatus() == IncidenceStatus.MANUAL_REVIEW_REQUIRED);

        String incidenceCode = hasIncidence ? incidenceOpt.get().getCode() : null;
        String incidenceMessage = hasIncidence ? incidenceOpt.get().getMessage() : null;

        // Consultar estado de devolución
        String refundStatus = payment.getRefundStatus() != null ? payment.getRefundStatus().name() : "NOT_REQUIRED";
        Optional<PaymentRefundIntent> refundIntentOpt = refundIntentRepository.findByPaymentId(paymentId);
        if (refundIntentOpt.isPresent() && !"COMPLETED".equals(refundStatus)) {
            switch (refundIntentOpt.get().getStatus()) {
                case COMPLETED -> refundStatus = "COMPLETED";
                case PENDING, PROCESSING -> refundStatus = "PENDING";
                case FAILED -> refundStatus = "FAILED";
                default -> {}
            }
        }

        return new CustomerPaymentStatusResponse(
                payment.getId(),
                payment.getStatus().name(),
                orderCreated,
                orderNumber,
                orderStatus,
                hasIncidence,
                incidenceCode,
                incidenceMessage,
                refundStatus,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getCreatedAt(),
                payment.getApprovedAt()
        );
    }
}
