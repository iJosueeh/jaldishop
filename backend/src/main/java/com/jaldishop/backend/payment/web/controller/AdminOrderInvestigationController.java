package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.investigation.ManualOrderInvestigationService;
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

    public AdminOrderInvestigationController(ManualOrderInvestigationService service,
            PaymentReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
        this.service = service;
    }

    @PostMapping("/{attemptId}/reconcile-order")
    public OrderReconciliationCandidateResult reconcile(@PathVariable UUID attemptId) {
        return reconciliationService.reconcileOrderAttempt(attemptId);
    }

    @PostMapping("/{attemptId}/investigate-order")
    public ManualOrderInvestigationService.Result investigate(@PathVariable UUID attemptId) {
        return service.investigate(attemptId);
    }
}
