package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.investigation.ManualOrderInvestigationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/payments/attempts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderInvestigationController {
    private final ManualOrderInvestigationService service;

    public AdminOrderInvestigationController(ManualOrderInvestigationService service) {
        this.service = service;
    }

    @PostMapping("/{attemptId}/investigate-order")
    public ManualOrderInvestigationService.Result investigate(@PathVariable UUID attemptId) {
        return service.investigate(attemptId);
    }
}
