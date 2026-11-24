package com.jaldishop.backend.payment.domain.port;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PaymentAuditPort {

    record HistoryItem(
            UUID attemptId,
            String action,
            String outcome,
            String reason,
            boolean reconciled,
            String orderNumber,
            String actorEmail,
            Instant timestamp
    ) {}

    void recordInvestigation(UUID actorId, UUID attemptId, String outcome, String reason, String ipAddress);

    void recordReconciliation(UUID actorId, UUID attemptId, String outcome, String reason, boolean reconciled, String orderNumber, String ipAddress);

    List<HistoryItem> findRecentHistory(int limit);
}
