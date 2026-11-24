package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.investigation.ManualOrderInvestigationService;
import com.jaldishop.backend.payment.application.reconciliation.OrderReconciliationCandidateResult;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService;
import com.jaldishop.backend.payment.domain.port.PaymentAuditPort;
import com.jaldishop.backend.payment.web.dto.PaymentInvestigationHistoryItemResponse;
import com.jaldishop.backend.payment.web.dto.ReconciliationCandidateItemResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.security.ratelimit.AuthenticatedRateLimits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments/attempts")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminOrderInvestigationController {
    private final ManualOrderInvestigationService service;
    private final PaymentReconciliationService reconciliationService;
    private final AuthenticatedRateLimits rateLimits;
    private final PaymentAuditPort paymentAuditPort;

    public AdminOrderInvestigationController(
            ManualOrderInvestigationService service,
            PaymentReconciliationService reconciliationService,
            AuthenticatedRateLimits rateLimits
    ) {
        this(service, reconciliationService, rateLimits, null);
    }

    @Autowired
    public AdminOrderInvestigationController(
            ManualOrderInvestigationService service,
            PaymentReconciliationService reconciliationService,
            AuthenticatedRateLimits rateLimits,
            PaymentAuditPort paymentAuditPort
    ) {
        this.reconciliationService = reconciliationService;
        this.service = service;
        this.rateLimits = rateLimits;
        this.paymentAuditPort = paymentAuditPort;
    }

    @PostMapping("/{attemptId}/reconcile-order")
    public OrderReconciliationCandidateResult reconcile(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        rateLimits.adminPaymentOperation(principal.userId());
        OrderReconciliationCandidateResult result = reconciliationService.reconcileOrderAttempt(attemptId);
        if (paymentAuditPort != null) {
            boolean reconciled = result.status() == OrderReconciliationCandidateResult.Status.SUCCESS_CONFIRMED
                    || result.status() == OrderReconciliationCandidateResult.Status.SUCCESS_ORDER_OPEN;
            paymentAuditPort.recordReconciliation(
                    principal.userId(),
                    attemptId,
                    result.status().name(),
                    result.detail(),
                    reconciled,
                    result.providerOrderId(),
                    resolveClientIp()
            );
        }
        return result;
    }

    @PostMapping("/{attemptId}/investigate-order")
    public ManualOrderInvestigationService.Result investigate(
            @PathVariable UUID attemptId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        rateLimits.adminPaymentOperation(principal.userId());
        ManualOrderInvestigationService.Result result = service.investigate(attemptId);
        if (paymentAuditPort != null) {
            paymentAuditPort.recordInvestigation(
                    principal.userId(),
                    attemptId,
                    result.outcome().name(),
                    result.reason(),
                    resolveClientIp()
            );
        }
        return result;
    }

    private String resolveClientIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();
        }
        return null;
    }

    @GetMapping("/candidates")
    public List<ReconciliationCandidateItemResponse> getCandidates(
            @RequestParam(defaultValue = "15") int limit,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        rateLimits.adminPaymentOperation(principal.userId());
        return reconciliationService.findOrphanCandidates(limit);
    }

    @GetMapping("/history")
    public List<PaymentInvestigationHistoryItemResponse> getHistory(
            @RequestParam(defaultValue = "10") @Min(1) @Max(20) int limit,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        rateLimits.adminPaymentHistoryRead(principal.userId());
        if (paymentAuditPort == null) {
            return List.of();
        }
        return paymentAuditPort.findRecentHistory(limit).stream()
                .map(PaymentInvestigationHistoryItemResponse::fromDomain)
                .toList();
    }
}
