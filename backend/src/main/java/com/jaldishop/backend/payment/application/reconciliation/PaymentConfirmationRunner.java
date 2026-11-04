package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Ejecutor transaccional atómico de la confirmación comercial para pagos conciliados.
 *
 * <p>Orden canónico de bloqueos garantizado:
 * 1. Payment (bloqueo pesimista PESSIMISTIC_WRITE mediante findByIdForUpdate).
 * 2. CapacityReservation (adquirido internamente por ConfirmPurchaseService).
 * 3. Inventory (adquirido internamente ordenado por variant_id ASC).</p>
 */
@Service
public class PaymentConfirmationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationRunner.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ConfirmPurchaseService confirmPurchaseService;
    private final PaymentIncidenceRepository incidenceRepository;

    @Autowired
    public PaymentConfirmationRunner(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ConfirmPurchaseService confirmPurchaseService,
            PaymentIncidenceRepository incidenceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.confirmPurchaseService = confirmPurchaseService;
        this.incidenceRepository = incidenceRepository;
    }

    public PaymentConfirmationRunner(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ConfirmPurchaseService confirmPurchaseService
    ) {
        this(paymentRepository, orderRepository, confirmPurchaseService, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderConfirmation confirmUnderLock(UUID paymentId) {
        // 1. Bloqueo pesimista del Payment (serializa cualquier competidor)
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado para conciliación: " + paymentId));

        // 2. Revalidaciones financieras y comerciales post-bloqueo
        if (payment.getStatus() != PaymentStatus.APPROVED) {
            log.info("Conciliación omitida: el pago {} no se encuentra APPROVED (estado actual: {})",
                    paymentId, payment.getStatus());
            return null;
        }

        if (payment.getRefundStatus() != RefundStatus.NOT_REQUIRED) {
            log.info("Conciliación omitida: el pago {} tiene refundStatus {} (distinto de NOT_REQUIRED)",
                    paymentId, payment.getRefundStatus());
            return null;
        }

        // 3. Revalidación de elegibilidad operativa de la incidencia post-bloqueo
        if (incidenceRepository != null) {
            Optional<PaymentIncidence> activeIncidenceOpt = incidenceRepository.findActiveByPaymentIdAndProcessType(
                    paymentId, IncidenceProcessType.ORDER_CONFIRMATION
            );
            if (activeIncidenceOpt.isPresent()) {
                PaymentIncidence activeIncidence = activeIncidenceOpt.get();
                if (activeIncidence.getStatus() == IncidenceStatus.MANUAL_REVIEW_REQUIRED) {
                    log.info("Conciliación omitida: el pago {} requiere revisión manual (MANUAL_REVIEW_REQUIRED)", paymentId);
                    return null;
                }
                if (activeIncidence.getNextRetryAt() != null && activeIncidence.getNextRetryAt().isAfter(Instant.now())) {
                    log.info("Conciliación omitida: el pago {} tiene reintento vigente a futuro (nextRetryAt: {})",
                            paymentId, activeIncidence.getNextRetryAt());
                    return null;
                }
                if (activeIncidence.getAttemptsCount() >= activeIncidence.getMaxAttempts()) {
                    log.info("Conciliación omitida: el pago {} ha superado el máximo de intentos permitidos ({})",
                            paymentId, activeIncidence.getMaxAttempts());
                    return null;
                }
            }
        }

        // 4. Idempotencia: Verificar si una carrera concurrente ya creó la orden
        Optional<Order> existingOrder = orderRepository.findByPaymentId(paymentId);
        if (existingOrder.isPresent()) {
            Order order = existingOrder.get();
            log.info("Idempotencia en conciliación: el pago {} ya tiene asociada la orden {}",
                    paymentId, order.getOrderNumber());
            return new OrderConfirmation(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getStatus(),
                    order.getStoreId(),
                    "",
                    order.getCustomerName(),
                    order.getTotalAmount(),
                    order.getCurrency(),
                    order.getConfirmedAt()
            );
        }

        // 5. Ejecutar ConfirmPurchaseService dentro de la misma transacción comercial
        ConfirmPurchaseCommand command = new ConfirmPurchaseCommand(paymentId);
        OrderConfirmation confirmation = confirmPurchaseService.execute(command);
        log.info("Conciliación exitosa: orden {} creada para el pago huérfano {}",
                confirmation.orderNumber(), paymentId);
        return confirmation;
    }
}
