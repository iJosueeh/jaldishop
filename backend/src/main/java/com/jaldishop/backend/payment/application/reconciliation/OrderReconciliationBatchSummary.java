package com.jaldishop.backend.payment.application.reconciliation;

import java.util.List;

public record OrderReconciliationBatchSummary(
        int totalCandidates,
        int successCount,
        int skippedCount,
        int commercialIncidenceCount,
        int failureCount,
        List<OrderReconciliationCandidateResult> results
) {
    public static OrderReconciliationBatchSummary fromResults(List<OrderReconciliationCandidateResult> results) {
        int total = results.size();
        int success = 0;
        int skipped = 0;
        int incidence = 0;
        int failures = 0;

        for (OrderReconciliationCandidateResult r : results) {
            switch (r.status()) {
                case SUCCESS_CONFIRMED, SUCCESS_ALREADY_APPROVED, SUCCESS_ORDER_OPEN, SUCCESS_CANCELLED_OR_EXPIRED -> success++;
                case SKIPPED_ACTIVE_LEASE, SKIPPED_NOT_ELIGIBLE -> skipped++;
                case COMMERCIAL_INCIDENCE -> incidence++;
                case CLAIM_LOST, TECHNICAL_FAILURE -> failures++;
            }
        }

        return new OrderReconciliationBatchSummary(total, success, skipped, incidence, failures, results);
    }
}
