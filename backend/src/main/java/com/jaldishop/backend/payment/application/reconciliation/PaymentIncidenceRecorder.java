package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.payment.domain.enums.IncidenceCategory;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Registrador de incidencias resiliente ejecutado en transacciones independientes (REQUIRES_NEW).
 * Serializa las actualizaciones mediante bloqueo pesimista en Payment y PaymentIncidence,
 * evitando lost updates de attempts_count y carreras de inserción.
 */
@Service
public class PaymentIncidenceRecorder {

    private static final Logger log = LoggerFactory.getLogger(PaymentIncidenceRecorder.class);

    private final PaymentRepository paymentRepository;
    private final PaymentIncidenceRepository incidenceRepository;

    public PaymentIncidenceRecorder(
            PaymentRepository paymentRepository,
            PaymentIncidenceRepository incidenceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.incidenceRepository = incidenceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordCommercialOrIndeterminateConflict(UUID paymentId, ConflictException ex) {
        String code = ex.getCode() != null ? ex.getCode() : "CONFLICT";
        String message = ex.getMessage();
        Instant now = Instant.now();

        // 1. Serializar mediante bloqueo pesimista en Payment (orden canónico)
        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
        if (payment == null) {
            throw new IllegalStateException("Pago inexistente al registrar incidencia");
        }

        if (payment.getStatus() != PaymentStatus.APPROVED)
            throw new IllegalStateException("La incidencia requiere un pago aprobado");
        // Payment lock serializes duplicate notifications; refund and incidence commit atomically.
        if (payment.getRefundStatus() != RefundStatus.NOT_REQUIRED) {
            return incidenceRepository.findLatestByPaymentIdAndProcessType(paymentId, IncidenceProcessType.ORDER_CONFIRMATION)
                    .filter(i -> i.getCategory() == IncidenceCategory.COMMERCIAL).isPresent();
        }

        if (isDefinitiveCommercialFailure(code)) {
            log.warn("Registrando fallo comercial definitivo para el pago {}: [{}] {}", paymentId, code, message);

            payment.requestRefund(payment.getAmount(), "INCIDENCE:" + code);
            paymentRepository.save(payment);

            Optional<PaymentIncidence> activeOpt = incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(
                    paymentId, IncidenceProcessType.ORDER_CONFIRMATION
            );
            if (activeOpt.isPresent()) {
                PaymentIncidence incidence = activeOpt.get();
                incidence.markResolved(now);
                incidenceRepository.save(incidence);
            }
            // Payment lock prevents duplicate terminal records; errors must abort refund + incidence together.
            incidenceRepository.save(PaymentIncidence.createCommercialTerminal(
                    paymentId, IncidenceProcessType.ORDER_CONFIRMATION, code, message, now));
        } else {
            log.warn("Registrando conflicto indeterminado/operativo para el pago {}: [{}] {}", paymentId, code, message);
            recordRetryUnderLock(paymentId, IncidenceCategory.OPERATIONAL, code, message, now);
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTechnicalRetry(UUID paymentId, Exception ex) {
        String code = ex.getClass().getSimpleName();
        String message = ex.getMessage() != null ? ex.getMessage() : "Error técnico durante conciliación";
        Instant now = Instant.now();

        // 1. Serializar mediante bloqueo pesimista en Payment (orden canónico)
        Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
        if (payment == null) {
            log.warn("No se pudo bloquear el pago {} para registrar reintento técnico: pago inexistente", paymentId);
            return;
        }

        log.warn("Registrando reintento técnico para el pago {}: [{}] {}", paymentId, code, message);
        recordRetryUnderLock(paymentId, IncidenceCategory.TECHNICAL, code, message, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markResolved(UUID paymentId) {
        // Bloquear primero el pago para mantener orden consistente
        paymentRepository.findByIdForUpdate(paymentId);

        Optional<PaymentIncidence> activeOpt = incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(
                paymentId, IncidenceProcessType.ORDER_CONFIRMATION
        );
        if (activeOpt.isPresent()) {
            PaymentIncidence incidence = activeOpt.get();
            incidence.markResolved(Instant.now());
            incidenceRepository.save(incidence);
            log.info("Incidencia de confirmación cerrada como RESOLVED para el pago {}", paymentId);
        }
    }

    private void recordRetryUnderLock(UUID paymentId, IncidenceCategory category, String code, String message, Instant now) {
        Optional<PaymentIncidence> activeOpt = incidenceRepository.findActiveByPaymentIdAndProcessTypeForUpdate(
                paymentId, IncidenceProcessType.ORDER_CONFIRMATION
        );

        if (activeOpt.isPresent()) {
            PaymentIncidence incidence = activeOpt.get();
            int nextAttempt = incidence.getAttemptsCount() + 1;
            Duration backoff = calculateBackoff(nextAttempt);
            incidence.recordRetry(backoff, code, message, now);
            incidenceRepository.save(incidence);
            checkManualReview(paymentId, incidence, code, message);
        } else {
            PaymentIncidence incidence = PaymentIncidence.createNew(
                    paymentId, IncidenceProcessType.ORDER_CONFIRMATION, category, code, message,
                    calculateBackoff(1), now);
            incidenceRepository.save(incidence);
        }
    }

    private void checkManualReview(UUID paymentId, PaymentIncidence incidence, String code, String message) {
        if (incidence.getStatus() == com.jaldishop.backend.payment.domain.enums.IncidenceStatus.MANUAL_REVIEW_REQUIRED) {
            log.error("ALERTA OPERATIVA: El pago {} ha agotado {} intentos y requiere REVISIÓN MANUAL. Último error: [{}] {}",
                    paymentId, incidence.getMaxAttempts(), code, message);
        }
    }

    public static boolean isDefinitiveCommercialFailure(String code) {
        if (code == null) return false;
        return switch (code) {
            case "RESERVATION_PROTECTION_EXPIRED",
                 "RESERVATION_EXPIRED",
                 "RESERVATION_RELEASED",
                 "INSUFFICIENT_STOCK",
                 "STORE_INACTIVE",
                 "INVALID_SNAPSHOT",
                 "DELIVERY_NOT_AVAILABLE",
                 "PICKUP_NOT_AVAILABLE",
                 "CART_EMPTY" -> true;
            default -> false;
        };
    }

    public static Duration calculateBackoff(int attemptNumber) {
        return switch (attemptNumber) {
            case 1 -> Duration.ofMinutes(5);
            case 2 -> Duration.ofMinutes(15);
            case 3 -> Duration.ofMinutes(30);
            case 4 -> Duration.ofMinutes(60);
            default -> Duration.ofHours(2);
        };
    }
}
