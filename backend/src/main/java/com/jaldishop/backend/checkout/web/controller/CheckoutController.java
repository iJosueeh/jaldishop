package com.jaldishop.backend.checkout.web.controller;

import com.jaldishop.backend.checkout.application.CheckoutResult;
import com.jaldishop.backend.checkout.application.CheckoutService;
import com.jaldishop.backend.checkout.application.InitiateCheckoutCommand;
import com.jaldishop.backend.checkout.web.dto.CheckoutResponse;
import com.jaldishop.backend.checkout.web.dto.InitiateCheckoutRequest;
import com.jaldishop.backend.checkout.web.mapper.CheckoutResponseMapper;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CheckoutResponseMapper responseMapper;

    public CheckoutController(CheckoutService checkoutService, CheckoutResponseMapper responseMapper) {
        this.checkoutService = checkoutService;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    public ResponseEntity<CheckoutResponse> initiateCheckout(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody InitiateCheckoutRequest request
    ) {
        var command = new InitiateCheckoutCommand(
                principal.userId(),
                request.storeId(),
                request.fulfillmentType(),
                request.serviceDate(),
                request.startTime(),
                request.endTime(),
                request.customerName(),
                request.customerPhone(),
                request.customerEmail(),
                request.deliveryAddress(),
                request.deliveryReference(),
                request.deliveryLatitude(),
                request.deliveryLongitude(),
                request.discountCode()
        );

        CheckoutResult result = checkoutService.initiateCheckout(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(result));
    }
}
