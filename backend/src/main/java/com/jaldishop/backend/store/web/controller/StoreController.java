package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.*;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final CreateStoreService createStoreService;
    private final GetMyStoreService getMyStoreService;
    private final UpdateStoreService updateStoreService;
    private final GetStoreBySlugService getStoreBySlugService;
    private final SearchPublicStoresService searchPublicStoresService;
    private final CloseMyStoreService closeMyStoreService;

    public StoreController(
            CreateStoreService createStoreService,
            GetMyStoreService getMyStoreService,
            UpdateStoreService updateStoreService,
            GetStoreBySlugService getStoreBySlugService,
            SearchPublicStoresService searchPublicStoresService,
            CloseMyStoreService closeMyStoreService
    ) {
        this.createStoreService = createStoreService;
        this.getMyStoreService = getMyStoreService;
        this.updateStoreService = updateStoreService;
        this.getStoreBySlugService = getStoreBySlugService;
        this.searchPublicStoresService = searchPublicStoresService;
        this.closeMyStoreService = closeMyStoreService;
    }

    @PostMapping()
    public ResponseEntity<StoreResponse> createStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateStoreRequest request
    ) {
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
                request.taxRate(),
                request.logoUrl(),
                request.bannerUrl(),
                request.instagramUrl(),
                request.facebookUrl(),
                request.whatsappNumber(),
                request.categoryIds()
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
                request.taxRate(),
                request.logoUrl(),
                request.bannerUrl(),
                request.instagramUrl(),
                request.facebookUrl(),
                request.whatsappNumber(),
                request.categoryIds()
        );

        Store store = updateStoreService.execute(command);
        return ResponseEntity.ok(StoreResponse.fromDomain(store));
    }

    @DeleteMapping("/me")
    public ResponseEntity<CloseStoreResponse> closeMyStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody(required = false) CloseStoreRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        CloseStoreResult result = closeMyStoreService.execute(new CloseMyStoreCommand(principal.userId(), reason));
        return ResponseEntity.ok(CloseStoreResponse.fromResult(result));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PublicStoreResponse> getStoreBySlug(
            @PathVariable String slug
    ) {
        Store store = getStoreBySlugService.execute(slug);
        return ResponseEntity.ok(PublicStoreResponse.fromDomain(store));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PublicStoreResponse>> searchPublicStores(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "5") int limit
    ) {
        List<PublicStoreResponse> stores = searchPublicStoresService.execute(q).stream()
                .limit(limit)
                .map(PublicStoreResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<PublicStoreResponse>> getFeaturedStores(
            @RequestParam(required = false, defaultValue = "6") int limit
    ) {
        List<PublicStoreResponse> stores = searchPublicStoresService.execute("").stream()
                .limit(limit)
                .map(PublicStoreResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(stores);
    }

}
