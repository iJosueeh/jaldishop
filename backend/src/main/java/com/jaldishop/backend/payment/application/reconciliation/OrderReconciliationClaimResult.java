package com.jaldishop.backend.payment.application.reconciliation;

import java.util.UUID;

public record OrderReconciliationClaimResult(
        boolean claimed,
        String skipReason,
        UUID attemptId,
        UUID paymentId,
        UUID workerToken,
        String providerOrderId
) {
    public static OrderReconciliationClaimResult claimed(
            UUID attemptId, UUID paymentId, UUID workerToken, String providerOrderId
    ) {
        return new OrderReconciliationClaimResult(true, null, attemptId, paymentId, workerToken, providerOrderId);
    }

    public static OrderReconciliationClaimResult skipped(String reason) {
        return new OrderReconciliationClaimResult(false, reason, null, null, null, null);
    }
}
