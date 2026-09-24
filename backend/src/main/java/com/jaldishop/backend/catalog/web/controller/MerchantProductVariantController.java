package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductVariantCommand;
import com.jaldishop.backend.catalog.application.ProductVariantService;
import com.jaldishop.backend.catalog.application.UpdateProductVariantCommand;
import com.jaldishop.backend.catalog.domain.VariantAttribute;
import com.jaldishop.backend.catalog.web.dto.CreateProductVariantRequest;
import com.jaldishop.backend.catalog.web.dto.ProductVariantResponse;
import com.jaldishop.backend.catalog.web.dto.UpdateProductVariantRequest;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/products/{productId}/variants")
public class MerchantProductVariantController {

    private final ProductVariantService variantService;
    private final StoreContextService storeContextService;

    public MerchantProductVariantController(ProductVariantService variantService, StoreContextService storeContextService) {
        this.variantService = variantService;
        this.storeContextService = storeContextService;
    }

    private void validateStoreOwnership(UUID storeId, JwtPrincipal principal) {
        UUID authenticatedStoreId = storeContextService.requireStoreId(principal);
        if (!storeId.equals(authenticatedStoreId)) {
            throw new AccessDeniedException("No tienes permisos para gestionar recursos de esta tienda.");
        }
    }

    @PostMapping
    public ResponseEntity<ProductVariantResponse> createVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request
    ) {
        validateStoreOwnership(storeId, principal);
        List<VariantAttribute> attributes = request.attributes() != null
                ? request.attributes().stream()
                .map(attr -> new VariantAttribute(attr.name(), attr.value()))
                .collect(Collectors.toList())
                : Collections.emptyList();

        var command = new CreateProductVariantCommand(
                storeId,
                productId,
                request.presentationName(),
                request.sku(),
                request.priceAmount(),
                request.priceCurrency(),
                request.tracksInventory(),
                attributes
        );

        var created = variantService.createVariant(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductVariantResponse.fromDomain(created));
    }

    @GetMapping
    public ResponseEntity<List<ProductVariantResponse>> getVariants(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    ) {
        validateStoreOwnership(storeId, principal);
        var list = variantService.getVariantsByProduct(productId, storeId).stream()
                .map(ProductVariantResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> getVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId
    ) {
        validateStoreOwnership(storeId, principal);
        var variant = variantService.getVariantById(variantId, productId, storeId);
        return ResponseEntity.ok(ProductVariantResponse.fromDomain(variant));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateProductVariantRequest request
    ) {
        validateStoreOwnership(storeId, principal);
        List<VariantAttribute> attributes = request.attributes() != null
                ? request.attributes().stream()
                .map(attr -> new VariantAttribute(attr.name(), attr.value()))
                .collect(Collectors.toList())
                : Collections.emptyList();

        var command = new UpdateProductVariantCommand(
                variantId,
                storeId,
                productId,
                request.presentationName(),
                request.sku(),
                request.priceAmount(),
                request.priceCurrency(),
                request.tracksInventory(),
                request.status(),
                attributes
        );

        var updated = variantService.updateVariant(command);
        return ResponseEntity.ok(ProductVariantResponse.fromDomain(updated));
    }
}