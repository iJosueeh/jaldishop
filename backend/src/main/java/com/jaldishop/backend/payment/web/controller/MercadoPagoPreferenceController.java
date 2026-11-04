package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceCommand;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceResult;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceService;
import com.jaldishop.backend.payment.web.dto.CreatePaymentPreferenceRequest;
import com.jaldishop.backend.payment.web.dto.CreatePaymentPreferenceResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout/mercado-pago/preference")
public class MercadoPagoPreferenceController {

    private final CreatePaymentPreferenceService service;

    public MercadoPagoPreferenceController(CreatePaymentPreferenceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CreatePaymentPreferenceResponse> createPreference(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreatePaymentPreferenceRequest request
    ) {
        CreatePaymentPreferenceResult result = service.execute(new CreatePaymentPreferenceCommand(
                principal.userId(),
                request.capacityReservationId(),
                request.fulfillmentType(),
                request.customerName(),
                request.customerPhone(),
                request.customerEmail(),
                request.deliveryAddress(),
                request.deliveryReference(),
                request.deliveryLatitude(),
                request.deliveryLongitude()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatePaymentPreferenceResponse(
                result.paymentId(),
                result.preferenceId(),
                result.initPoint(),
                result.status()
        ));
    }

}
