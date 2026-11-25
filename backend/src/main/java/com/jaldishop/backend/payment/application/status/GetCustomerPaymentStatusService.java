package com.jaldishop.backend.payment.application.status;

import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundIntentStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.model.PaymentRefundIntent;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRefundIntentRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.web.dto.CustomerPaymentStatusResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class GetCustomerPaymentStatusService {

    private static final Logger log = LoggerFactory.getLogger(GetCustomerPaymentStatusService.class);

    private final PaymentRepository paymentRepository;
    private final CapacityReservationRepository capacityReservationRepository;
    private final OrderRepository orderRepository;
    private final PaymentIncidenceRepository paymentIncidenceRepository;
    private final PaymentRefundIntentRepository refundIntentRepository;

    @Autowired(required = false)
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired(required = false)
    private PaymentReconciliationService paymentReconciliationService;

    @Autowired
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

        // Conciliación activa bajo demanda cuando el webhook aún no llegó (ej. entorno local o demora de pasarela)
        Optional<Order> orderOpt = orderRepository.findByPaymentId(paymentId);
        if (orderOpt.isEmpty()
                && (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.APPROVED)
                && paymentAttemptRepository != null
                && paymentReconciliationService != null) {
            try {
                var attempts = paymentAttemptRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId);
                for (int i = attempts.size() - 1; i >= 0; i--) {
                    PaymentAttempt attempt = attempts.get(i);
                    if ("ORDERS".equalsIgnoreCase(attempt.getCheckoutMode())
                            && attempt.getProviderOrderId() != null
                            && !attempt.getProviderOrderId().isBlank()) {
                        paymentReconciliationService.reconcileOrderAttempt(attempt.getId());
                        payment = paymentRepository.findById(paymentId).orElse(payment);
                        orderOpt = orderRepository.findByPaymentId(paymentId);
                        break;
                    }
                }
            } catch (Exception ex) {
                log.warn("Conciliación bajo demanda omitida para pago {}: {}", paymentId, ex.getMessage());
            }
        }

        // Consultar orden asociada
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
        String incidenceMessage = hasIncidence
                ? resolveCustomerIncidenceMessage(incidenceOpt.get().getCode(), incidenceOpt.get().getMessage())
                : null;

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

    public UUID resolvePaymentIdByAttempt(UUID attemptId, UUID authenticatedUserId) {
        Objects.requireNonNull(attemptId, "attemptId no puede ser nulo");
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId no puede ser nulo");

        if (paymentAttemptRepository == null) {
            throw new ResourceNotFoundException("Intento de pago no encontrado: " + attemptId);
        }

        PaymentAttempt attempt = paymentAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Intento de pago no encontrado: " + attemptId));

        Payment payment = paymentRepository.findById(attempt.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado: " + attempt.getPaymentId()));

        CapacityReservation reservation = capacityReservationRepository.findById(payment.getCapacityReservationId())
                .orElse(null);

        if (reservation == null || !reservation.getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("No tienes autorización para consultar este pago.");
        }

        return payment.getId();
    }

    private String resolveCustomerIncidenceMessage(String code, String rawMessage) {
        if (code == null) {
            return "Ocurrió una incidencia al procesar tu pedido. Tu pago se encuentra bajo revisión.";
        }
        return switch (code) {
            case "RESERVATION_EXPIRED", "PAYMENT_APPROVED_AFTER_RESERVATION_EXPIRED" ->
                    "La reserva de horario ha vencido. Se gestionará la devolución correspondiente.";
            case "INVENTORY_RESERVATION_EXPIRED", "INSUFFICIENT_STOCK" ->
                    "No hay disponibilidad suficiente para los productos seleccionados. Se gestionará la devolución correspondiente.";
            case "RESERVATION_PROTECTION_EXPIRED" ->
                    "El tiempo límite para confirmar el pedido ha vencido. Se gestionará la devolución correspondiente.";
            default ->
                    "Ocurrió una incidencia al procesar tu pedido. Tu pago se encuentra bajo revisión.";
        };
    }
}
