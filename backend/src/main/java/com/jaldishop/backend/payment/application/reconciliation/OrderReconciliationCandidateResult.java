package com.jaldishop.backend.payment.application.reconciliation;

import java.util.UUID;

public record OrderReconciliationCandidateResult(
        UUID attemptId,
        UUID paymentId,
        String providerOrderId,
        Status status,
        String detail,
        String financialStatus,
        CommercialStatus commercialStatus
) {
    public enum CommercialStatus { CONFIRMED, INCIDENCE_RECORDED, PENDING, NOT_ATTEMPTED }

    public OrderReconciliationCandidateResult(UUID attemptId, UUID paymentId, String providerOrderId, Status status, String detail) {
        this(attemptId, paymentId, providerOrderId, status, detail, null, switch (status) {
            case SUCCESS_CONFIRMED -> CommercialStatus.CONFIRMED;
            case COMMERCIAL_INCIDENCE -> CommercialStatus.INCIDENCE_RECORDED;
            case APPROVED_COMMERCIAL_PENDING -> CommercialStatus.PENDING;
            default -> CommercialStatus.NOT_ATTEMPTED;
        });
    }

    public static OrderReconciliationCandidateResult commercialPending(UUID attemptId, UUID paymentId, String orderId) {
        return new OrderReconciliationCandidateResult(attemptId, paymentId, orderId, Status.APPROVED_COMMERCIAL_PENDING,
                "Pago aprobado; confirmación comercial pendiente o requiere revisión");
    }

    public enum Status {
        APPROVED_COMMERCIAL_PENDING,
        SUCCESS_CONFIRMED,
        SUCCESS_ORDER_OPEN,
        SUCCESS_CANCELLED_OR_EXPIRED,
        COMMERCIAL_INCIDENCE,
        SKIPPED_ACTIVE_LEASE,
        SKIPPED_NOT_ELIGIBLE,
        CLAIM_LOST,
        TECHNICAL_FAILURE
    }

    public static OrderReconciliationCandidateResult successConfirmed(UUID attemptId, UUID paymentId, String orderId, String orderNumber) {
        return new OrderReconciliationCandidateResult(attemptId, paymentId, orderId, Status.SUCCESS_CONFIRMED, "Pedido confirmado: " + orderNumber);
    }

    public static OrderReconciliationCandidateResult orderOpen(UUID attemptId, UUID paymentId, String orderId) {
        return new OrderReconciliationCandidateResult(attemptId, paymentId, orderId, Status.SUCCESS_ORDER_OPEN, "Orden remota aún creada y abierta");
    }

    public static OrderReconciliationCandidateResult cancelledOrExpired(UUID attemptId, UUID paymentId, String orderId, String reason) {
        return new OrderReconciliationCandidateResult(attemptId, paymentId, orderId, Status.SUCCESS_CANCELLED_OR_EXPIRED, "Orden cancelada/expirada: " + reason);
    }

    public static OrderReconciliationCandidateResult commercialIncidence(UUID attemptId, UUID paymentId, String orderId, String reason) {
        return new OrderReconciliationCandidateResult(attemptId, paymentId, orderId, Status.COMMERCIAL_INCIDENCE, "Incidencia comercial registrada: " + reason);
    }

    public static OrderReconciliationCandidateResult skipped(UUID attemptId, String reason) {
        return new OrderReconciliationCandidateResult(attemptId, null, null, Status.SKIPPED_NOT_ELIGIBLE, reason);
    }

    public static OrderReconciliationCandidateResult claimLost(UUID attemptId, UUID paymentId, String reason) {
        return new OrderReconciliationCandidateResult(attemptId, paymentId, null, Status.CLAIM_LOST, reason);
    }

    public static OrderReconciliationCandidateResult technicalFailure(UUID attemptId, UUID paymentId, String orderId, String error) {
        return new OrderReconciliationCandidateResult(attemptId, paymentId, orderId, Status.TECHNICAL_FAILURE, error);
    }
}
