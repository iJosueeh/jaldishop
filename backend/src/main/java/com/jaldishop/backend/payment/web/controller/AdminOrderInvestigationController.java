package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.investigation.ManualOrderInvestigationService;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.security.ratelimit.AuthenticatedRateLimits;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.jaldishop.backend.payment.application.reconciliation.OrderReconciliationCandidateResult;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments/attempts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderInvestigationController {
    private final ManualOrderInvestigationService service;
    private final PaymentReconciliationService reconciliationService;
    private final AuthenticatedRateLimits rateLimits;

    public AdminOrderInvestigationController(ManualOrderInvestigationService service,
            PaymentReconciliationService reconciliationService, AuthenticatedRateLimits rateLimits) {
        this.reconciliationService = reconciliationService;
        this.service = service;
        this.rateLimits = rateLimits;
    }

    @PostMapping("/{attemptId}/reconcile-order")
    public OrderReconciliationCandidateResult reconcile(@PathVariable UUID attemptId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        rateLimits.adminPaymentOperation(principal.userId());
        return reconciliationService.reconcileOrderAttempt(attemptId);
    }

    @PostMapping("/{attemptId}/investigate-order")
    public ManualOrderInvestigationService.Result investigate(@PathVariable UUID attemptId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        rateLimits.adminPaymentOperation(principal.userId());
        return service.investigate(attemptId);
    }
}
