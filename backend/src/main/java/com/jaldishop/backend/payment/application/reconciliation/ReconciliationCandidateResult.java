package com.jaldishop.backend.payment.application.reconciliation;

import java.util.UUID;

public record ReconciliationCandidateResult(
        UUID paymentId,
        Status status,
        String orderNumber,
        String detail
) {
    public enum Status {
        SUCCESS,
        SKIPPED,
        COMMERCIAL_FAILURE,
        INDETERMINATE_CONFLICT,
        TRANSIENT_FAILURE,
        UNEXPECTED_FAILURE
    }

    public static ReconciliationCandidateResult success(UUID paymentId, String orderNumber) {
        return new ReconciliationCandidateResult(paymentId, Status.SUCCESS, orderNumber, "Orden confirmada exitosamente");
    }

    public static ReconciliationCandidateResult skipped(UUID paymentId, String reason) {
        return new ReconciliationCandidateResult(paymentId, Status.SKIPPED, null, reason);
    }

    public static ReconciliationCandidateResult conflict(UUID paymentId, String code, String message) {
        Status status = PaymentIncidenceRecorder.isDefinitiveCommercialFailure(code)
                ? Status.COMMERCIAL_FAILURE
                : Status.INDETERMINATE_CONFLICT;
        return new ReconciliationCandidateResult(paymentId, status, null, "[" + code + "] " + message);
    }

    public static ReconciliationCandidateResult transientFailure(UUID paymentId, String message) {
        return new ReconciliationCandidateResult(paymentId, Status.TRANSIENT_FAILURE, null, message);
    }

    public static ReconciliationCandidateResult technicalFailure(UUID paymentId, String message) {
        return new ReconciliationCandidateResult(paymentId, Status.UNEXPECTED_FAILURE, null, message);
    }
}
