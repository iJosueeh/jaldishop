package com.jaldishop.backend.ordering.web.controller;

import com.jaldishop.backend.ordering.application.GetCustomerOrderDetailService;
import com.jaldishop.backend.ordering.application.GetCustomerOrdersService;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderDetailResponse;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderSummaryResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/orders")
public class CustomerOrderController {

    private final GetCustomerOrdersService getCustomerOrdersService;
    private final GetCustomerOrderDetailService getCustomerOrderDetailService;

    public CustomerOrderController(GetCustomerOrdersService getCustomerOrdersService,
                                   GetCustomerOrderDetailService getCustomerOrderDetailService) {
        this.getCustomerOrdersService = getCustomerOrdersService;
        this.getCustomerOrderDetailService = getCustomerOrderDetailService;
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

    private void validateCustomerPrincipal(JwtPrincipal principal) {
        if (principal == null || !principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden acceder a sus pedidos.");
        }
    }

}
