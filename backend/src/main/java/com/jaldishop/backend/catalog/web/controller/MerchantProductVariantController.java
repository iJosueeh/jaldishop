package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductVariantCommand;
import com.jaldishop.backend.catalog.application.CreateProductVariantService;
import com.jaldishop.backend.catalog.application.GetProductVariantsService;
import com.jaldishop.backend.catalog.application.UpdateProductVariantCommand;
import com.jaldishop.backend.catalog.application.UpdateProductVariantService;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.VariantAttribute;
import com.jaldishop.backend.catalog.web.dto.CreateProductVariantRequest;
import com.jaldishop.backend.catalog.web.dto.ProductVariantResponse;
import com.jaldishop.backend.catalog.web.dto.UpdateProductVariantRequest;
import com.jaldishop.backend.catalog.web.mapper.ProductVariantResponseMapper;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/products/{productId}/variants")
@PreAuthorize("hasRole('MERCHANT')")
public class MerchantProductVariantController {

    private final CreateProductVariantService createVariantService;
    private final UpdateProductVariantService updateVariantService;
    private final GetProductVariantsService getVariantsService;
    private final StoreContextService storeContextService;
    private final ProductVariantResponseMapper responseMapper;

    public MerchantProductVariantController(
            CreateProductVariantService createVariantService,
            UpdateProductVariantService updateVariantService,
            GetProductVariantsService getVariantsService,
            StoreContextService storeContextService,
            ProductVariantResponseMapper responseMapper
    ) {
        this.createVariantService = createVariantService;
        this.updateVariantService = updateVariantService;
        this.getVariantsService = getVariantsService;
        this.storeContextService = storeContextService;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    public ResponseEntity<ProductVariantResponse> createVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @Valid @RequestBody CreateProductVariantRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        List<VariantAttribute> attributes = request.attributes() != null
                ? request.attributes().stream()
                .map(attr -> new VariantAttribute(attr.name(), attr.value()))
                .toList()
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

        ProductVariant created = createVariantService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<ProductVariantResponse>> getVariants(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        List<ProductVariant> list = getVariantsService.execute(productId, storeId);
        List<ProductVariantResponse> response = list.stream()
                .map(responseMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> getVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        ProductVariant variant = getVariantsService.execute(variantId, productId, storeId);
        return ResponseEntity.ok(responseMapper.toResponse(variant));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ProductVariantResponse> updateVariant(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateProductVariantRequest request
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        List<VariantAttribute> attributes = request.attributes() != null
                ? request.attributes().stream()
                .map(attr -> new VariantAttribute(attr.name(), attr.value()))
                .toList()
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

        ProductVariant updated = updateVariantService.execute(command);
        return ResponseEntity.ok(responseMapper.toResponse(updated));
    }
}