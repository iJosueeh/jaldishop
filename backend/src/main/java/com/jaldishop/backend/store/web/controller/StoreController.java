package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.*;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.web.dto.CreateStoreRequest;
import com.jaldishop.backend.store.web.dto.StoreResponse;
import com.jaldishop.backend.store.web.dto.UpdateStoreRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final CreateStoreService createStoreService;
    private final GetMyStoreService getMyStoreService;
    private final UpdateStoreService updateStoreService;

    public StoreController(CreateStoreService createStoreService, GetMyStoreService getMyStoreService, UpdateStoreService updateStoreService) {
        this.createStoreService = createStoreService;
        this.getMyStoreService = getMyStoreService;
        this.updateStoreService = updateStoreService;
    }

    @PostMapping()
    public ResponseEntity<StoreResponse> createStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateStoreRequest request
    ) {
        if (!principal.roles().contains("MERCHANT")) {
            throw new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden crear una tienda.");
        }

        CreateStoreCommand command = new CreateStoreCommand(
                principal.userId(),
                request.name(),
                request.slug(),
                request.description(),
                request.contactPhone(),
                request.address(),
                request.addressReference(),
                request.latitude(),
                request.longitude(),
                request.pickupEnabled(),
                request.deliveryEnabled(),
                request.deliveryFeeAmount(),
                request.deliveryFeeCurrency(),
                request.taxApplies(),
                request.taxRate()
        );

        Store store = createStoreService.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                StoreResponse.fromDomain(store)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<StoreResponse> getMyStore(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        Store store = getMyStoreService.execute(principal.userId());
        return ResponseEntity.ok(StoreResponse.fromDomain(store));
    }

    @PutMapping("/me")
    public ResponseEntity<StoreResponse> updateStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpdateStoreRequest request
    ) {
        UpdateStoreCommand command = new UpdateStoreCommand(
                principal.userId(),
                request.name(),
                request.description(),
                request.contactPhone(),
                request.address(),
                request.addressReference(),
                request.latitude(),
                request.longitude(),
                request.pickupEnabled(),
                request.deliveryEnabled(),
                request.deliveryFeeAmount(),
                request.deliveryFeeCurrency(),
                request.taxApplies(),
                request.taxRate()
        );

        Store store = updateStoreService.execute(command);
        return ResponseEntity.ok(StoreResponse.fromDomain(store));
    }

}
