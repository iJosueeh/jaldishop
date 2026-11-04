package com.jaldishop.backend.ordering.web.controller;

import com.jaldishop.backend.ordering.application.service.CancelCustomerOrderService;
import com.jaldishop.backend.ordering.application.service.GetCustomerOrderDetailService;
import com.jaldishop.backend.ordering.application.service.GetCustomerOrdersService;
import com.jaldishop.backend.ordering.web.dto.CancelOrderRequest;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderDetailResponse;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderSummaryResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/orders")
public class CustomerOrderController {

    private final GetCustomerOrdersService getCustomerOrdersService;
    private final GetCustomerOrderDetailService getCustomerOrderDetailService;
    private final CancelCustomerOrderService cancelCustomerOrderService;

    @Autowired
    public CustomerOrderController(
            GetCustomerOrdersService getCustomerOrdersService,
            GetCustomerOrderDetailService getCustomerOrderDetailService,
            CancelCustomerOrderService cancelCustomerOrderService
    ) {
        this.getCustomerOrdersService = getCustomerOrdersService;
        this.getCustomerOrderDetailService = getCustomerOrderDetailService;
        this.cancelCustomerOrderService = cancelCustomerOrderService;
    }

    public CustomerOrderController(
            GetCustomerOrdersService getCustomerOrdersService,
            GetCustomerOrderDetailService getCustomerOrderDetailService
    ) {
        this(getCustomerOrdersService, getCustomerOrderDetailService, null);
    }

    @GetMapping
    public ResponseEntity<List<CustomerOrderSummaryResponse>> getMyOrders(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        validateCustomerPrincipal(principal);
        List<CustomerOrderSummaryResponse> orders = getCustomerOrdersService.execute(principal.userId());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<CustomerOrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String orderNumber
    ) {
        validateCustomerPrincipal(principal);
        CustomerOrderDetailResponse order = getCustomerOrderDetailService.execute(principal.userId(), orderNumber);
        return ResponseEntity.ok(order);
    }

    @PatchMapping("/{orderNumber}/cancel")
    public ResponseEntity<CustomerOrderDetailResponse> cancelOrder(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable String orderNumber,
            @Valid @RequestBody(required = false) CancelOrderRequest request
    ) {
        validateCustomerPrincipal(principal);
        String reason = request != null ? request.reason() : null;
        CustomerOrderDetailResponse order = cancelCustomerOrderService.execute(principal.userId(), orderNumber, reason);
        return ResponseEntity.ok(order);
    }

    private void validateCustomerPrincipal(JwtPrincipal principal) {
        if (principal == null || !principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden acceder a sus pedidos.");
        }
    }

}
