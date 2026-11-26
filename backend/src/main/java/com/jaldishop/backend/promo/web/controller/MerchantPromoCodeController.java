package com.jaldishop.backend.promo.web.controller;

import com.jaldishop.backend.promo.application.dto.PromoCodeResponse;
import com.jaldishop.backend.promo.application.service.ManageMerchantPromoCodeService;
import com.jaldishop.backend.promo.web.dto.TogglePromoCodeRequest;
import com.jaldishop.backend.promo.web.dto.UpsertPromoCodeRequest;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/promo-codes")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantPromoCodeController {

    private final StoreContextService storeContextService;
    private final ManageMerchantPromoCodeService service;

    public MerchantPromoCodeController(
            StoreContextService storeContextService,
            ManageMerchantPromoCodeService service
    ) {
        this.storeContextService = storeContextService;
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PromoCodeResponse>> list(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        return ResponseEntity.ok(service.listByStore(storeId));
    }

    @PostMapping
    public ResponseEntity<PromoCodeResponse> create(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpsertPromoCodeRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(storeId, request.toCommand()));
    }

    @PutMapping("/{promoId}")
    public ResponseEntity<PromoCodeResponse> update(
            @PathVariable UUID storeId,
            @PathVariable UUID promoId,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody UpsertPromoCodeRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        return ResponseEntity.ok(service.update(storeId, promoId, request.toCommand()));
    }

    @PatchMapping("/{promoId}/toggle")
    public ResponseEntity<PromoCodeResponse> toggle(
            @PathVariable UUID storeId,
            @PathVariable UUID promoId,
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody TogglePromoCodeRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        return ResponseEntity.ok(service.toggleActive(storeId, promoId, request.active()));
    }

    @DeleteMapping("/{promoId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID storeId,
            @PathVariable UUID promoId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        service.delete(storeId, promoId);
        return ResponseEntity.noContent().build();
    }
}
