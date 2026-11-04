package com.jaldishop.backend.payment.application.reconciliation;

import java.util.List;

public record ReconciliationBatchSummary(
        int totalCandidates,
        int successCount,
        int skippedCount,
        int commercialFailuresCount,
        int technicalFailuresCount,
        List<ReconciliationCandidateResult> results
) {
    public static ReconciliationBatchSummary fromResults(List<ReconciliationCandidateResult> results) {
        int total = results.size();
        int success = (int) results.stream().filter(r -> r.status() == ReconciliationCandidateResult.Status.SUCCESS).count();
        int skipped = (int) results.stream().filter(r -> r.status() == ReconciliationCandidateResult.Status.SKIPPED).count();
        int commercial = (int) results.stream().filter(r -> r.status() == ReconciliationCandidateResult.Status.COMMERCIAL_FAILURE).count();
        int technical = (int) results.stream().filter(r -> r.status() == ReconciliationCandidateResult.Status.TRANSIENT_FAILURE
                || r.status() == ReconciliationCandidateResult.Status.UNEXPECTED_FAILURE
                || r.status() == ReconciliationCandidateResult.Status.INDETERMINATE_CONFLICT).count();

        return new ReconciliationBatchSummary(total, success, skipped, commercial, technical, results);
    }
}
