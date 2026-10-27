package com.jaldishop.backend.address.web.controller;

import com.jaldishop.backend.address.application.*;
import com.jaldishop.backend.address.web.dto.*;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAddressController {

    private final GetCustomerAddressesService getCustomerAddressesService;
    private final CreateCustomerAddressService createCustomerAddressService;
    private final UpdateCustomerAddressService updateCustomerAddressService;
    private final SetDefaultCustomerAddressService setDefaultCustomerAddressService;
    private final DeleteCustomerAddressService deleteCustomerAddressService;

    public CustomerAddressController(GetCustomerAddressesService getCustomerAddressesService,
                                     CreateCustomerAddressService createCustomerAddressService,
                                     UpdateCustomerAddressService updateCustomerAddressService,
                                     SetDefaultCustomerAddressService setDefaultCustomerAddressService,
                                     DeleteCustomerAddressService deleteCustomerAddressService) {
        this.getCustomerAddressesService = getCustomerAddressesService;
        this.createCustomerAddressService = createCustomerAddressService;
        this.updateCustomerAddressService = updateCustomerAddressService;
        this.setDefaultCustomerAddressService = setDefaultCustomerAddressService;
        this.deleteCustomerAddressService = deleteCustomerAddressService;
    }

    @GetMapping
    public ResponseEntity<CustomerAddressListResponse> getAddresses(
            @AuthenticationPrincipal JwtPrincipal principal) {
        CustomerAddressListResponse response = getCustomerAddressesService.execute(principal.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CustomerAddressResponse> createAddress(
            @Valid @RequestBody CreateCustomerAddressRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        CustomerAddressResponse response = createCustomerAddressService.execute(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerAddressResponse> updateAddress(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCustomerAddressRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        CustomerAddressResponse response = updateCustomerAddressService.execute(principal.userId(), id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<CustomerAddressResponse> setDefaultAddress(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        CustomerAddressResponse response = setDefaultCustomerAddressService.execute(principal.userId(), id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable("id") UUID id,
            @AuthenticationPrincipal JwtPrincipal principal) {
        deleteCustomerAddressService.execute(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
