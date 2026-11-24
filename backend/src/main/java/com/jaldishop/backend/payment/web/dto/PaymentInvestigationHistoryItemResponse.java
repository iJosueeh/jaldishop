package com.jaldishop.backend.payment.web.dto;

import com.jaldishop.backend.payment.domain.port.PaymentAuditPort;

import java.time.Instant;
import java.util.UUID;

public record PaymentInvestigationHistoryItemResponse(
        UUID attemptId,
        String action,
        String outcome,
        String reason,
        boolean reconciled,
        String orderNumber,
        String actorEmail,
        Instant timestamp
) {
    public static PaymentInvestigationHistoryItemResponse fromDomain(PaymentAuditPort.HistoryItem item) {
        return new PaymentInvestigationHistoryItemResponse(
                item.attemptId(),
                item.action(),
                item.outcome(),
                item.reason(),
                item.reconciled(),
                item.orderNumber(),
                item.actorEmail(),
                item.timestamp()
        );
    }
}
