package com.jaldishop.backend.payment.application.reconciliation;

import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orquestador no transaccional a nivel de candidato individual.
 * Delimita las llamadas entre PaymentConfirmationRunner y PaymentIncidenceRecorder
 * garantizando que ninguna excepción de confirmación aborte la trazabilidad.
 */
@Service
public class PaymentReconciliationProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationProcessor.class);

    private final PaymentConfirmationRunner confirmationRunner;
    private final PaymentIncidenceRecorder incidenceRecorder;

    public PaymentReconciliationProcessor(
            PaymentConfirmationRunner confirmationRunner,
            PaymentIncidenceRecorder incidenceRecorder
    ) {
        this.confirmationRunner = confirmationRunner;
        this.incidenceRecorder = incidenceRecorder;
    }

    public ReconciliationCandidateResult processCandidate(UUID paymentId) {
        try {
            OrderConfirmation confirmation = confirmationRunner.confirmUnderLock(paymentId);
            if (confirmation != null) {
                incidenceRecorder.markResolved(paymentId);
                return ReconciliationCandidateResult.success(paymentId, confirmation.orderNumber());
            }
            return ReconciliationCandidateResult.skipped(paymentId, "Omitido por revalidación o ya confirmado");
        } catch (ConflictException ex) {
            log.warn("Fallo por conflicto al conciliar pago {}: [{}] {}", paymentId, ex.getCode(), ex.getMessage());
            incidenceRecorder.recordCommercialOrIndeterminateConflict(paymentId, ex);
            return ReconciliationCandidateResult.conflict(paymentId, ex.getCode(), ex.getMessage());
        } catch (TransientDataAccessException ex) {
            log.warn("Fallo transitorio de base de datos al conciliar pago {}: {}", paymentId, ex.getMessage());
            incidenceRecorder.recordTechnicalRetry(paymentId, ex);
            return ReconciliationCandidateResult.transientFailure(paymentId, ex.getMessage());
        } catch (Exception ex) {
            log.error("Fallo inesperado al conciliar pago {}: {}", paymentId, ex.getMessage(), ex);
            incidenceRecorder.recordTechnicalRetry(paymentId, ex);
            return ReconciliationCandidateResult.technicalFailure(paymentId, ex.getMessage());
        }
    }
}
