package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.GetStoreCustomersService;
import com.jaldishop.backend.store.application.StoreContextService;
import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant/customers")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantCustomerController {

    private final StoreContextService storeContextService;
    private final GetStoreCustomersService getStoreCustomersService;

    public MerchantCustomerController(
            StoreContextService storeContextService,
            GetStoreCustomersService getStoreCustomersService
    ) {
        this.storeContextService = storeContextService;
        this.getStoreCustomersService = getStoreCustomersService;
    }

    @GetMapping
    public ResponseEntity<List<StoreCustomerResponse>> listCustomers(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String query
    ) {
        UUID storeId = storeContextService.requireStoreId(principal);
        List<StoreCustomerResponse> customers = getStoreCustomersService.execute(storeId, query);
        return ResponseEntity.ok(customers);
    }
}
