package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.status.GetCustomerPaymentStatusService;
import com.jaldishop.backend.payment.web.dto.CustomerPaymentStatusResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/payments")
public class CustomerPaymentStatusController {

    private final GetCustomerPaymentStatusService service;

    public CustomerPaymentStatusController(GetCustomerPaymentStatusService service) {
        this.service = service;
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<CustomerPaymentStatusResponse> getPaymentStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID paymentId
    ) {
        validateCustomerPrincipal(principal);
        CustomerPaymentStatusResponse response = service.execute(paymentId, principal.userId());
        return ResponseEntity.ok(response);
    }

    private void validateCustomerPrincipal(JwtPrincipal principal) {
        if (principal == null || !principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden consultar el estado de sus pagos.");
        }
    }
}
