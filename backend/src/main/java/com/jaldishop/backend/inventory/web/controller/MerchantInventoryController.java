package com.jaldishop.backend.inventory.web.controller;

import com.jaldishop.backend.inventory.application.service.GetLowStockInventoryService;
import com.jaldishop.backend.inventory.application.service.GetStoreInventoryService;
import com.jaldishop.backend.inventory.application.service.GetVariantInventoryService;
import com.jaldishop.backend.inventory.application.service.UpdateInventoryStockService;
import com.jaldishop.backend.inventory.application.service.UpdateInventoryThresholdService;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.dto.UpdateStockRequest;
import com.jaldishop.backend.inventory.web.dto.UpdateThresholdRequest;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST Merchant para el Módulo de Inventario (BE-13).
 * Todas las operaciones requieren autenticación con rol MERCHANT y están aisladas a la tienda del comerciante.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
public class MerchantInventoryController {

    private final StoreContextService storeContextService;
    private final GetStoreInventoryService getStoreInventoryService;
    private final GetVariantInventoryService getVariantInventoryService;
    private final GetLowStockInventoryService getLowStockInventoryService;
    private final UpdateInventoryStockService updateInventoryStockService;
    private final UpdateInventoryThresholdService updateInventoryThresholdService;

    public MerchantInventoryController(
            StoreContextService storeContextService,
            GetStoreInventoryService getStoreInventoryService,
            GetVariantInventoryService getVariantInventoryService,
            GetLowStockInventoryService getLowStockInventoryService,
            UpdateInventoryStockService updateInventoryStockService,
            UpdateInventoryThresholdService updateInventoryThresholdService
    ) {
        this.storeContextService = storeContextService;
        this.getStoreInventoryService = getStoreInventoryService;
        this.getVariantInventoryService = getVariantInventoryService;
        this.getLowStockInventoryService = getLowStockInventoryService;
        this.updateInventoryStockService = updateInventoryStockService;
        this.updateInventoryThresholdService = updateInventoryThresholdService;
    }

    /**
     * 1. GET /api/v1/inventory
     * Consulta el inventario general de todas las variantes de la tienda del comerciante.
     */
    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getStoreInventory(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID storeId = storeContextService.requireStoreId(principal);
        List<InventoryResponse> response = getStoreInventoryService.execute(storeId);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. GET /api/v1/inventory/variants/{variantId}
     * Consulta el estado de inventario para una variante específica de la tienda.
     */
    @GetMapping("/variants/{variantId}")
    public ResponseEntity<InventoryResponse> getVariantInventory(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId
    ) {
        UUID storeId = storeContextService.requireStoreId(principal);
        InventoryResponse response = getVariantInventoryService.execute(storeId, variantId);
        return ResponseEntity.ok(response);
    }

    /**
     * 3. GET /api/v1/inventory/low-stock
     * Consulta las variantes que se encuentran en estado de stock bajo (quantity <= lowStockThreshold).
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryResponse>> getLowStockInventory(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID storeId = storeContextService.requireStoreId(principal);
        List<InventoryResponse> response = getLowStockInventoryService.execute(storeId);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. PUT /api/v1/inventory/variants/{variantId}/stock
     * Configura el stock inicial o ajusta la cantidad física absoluta de una variante.
     */
    @PutMapping("/variants/{variantId}/stock")
    public ResponseEntity<InventoryResponse> updateStock(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateStockRequest request
    ) {
        UUID storeId = storeContextService.requireStoreId(principal);
        InventoryResponse response = updateInventoryStockService.execute(storeId, variantId, request.quantity());
        return ResponseEntity.ok(response);
    }

    /**
     * 5. PATCH /api/v1/inventory/variants/{variantId}/threshold
     * Configura o actualiza el umbral de alerta de stock bajo (o null para desactivarlo).
     */
    @PatchMapping("/variants/{variantId}/threshold")
    public ResponseEntity<InventoryResponse> updateThreshold(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateThresholdRequest request
    ) {
        UUID storeId = storeContextService.requireStoreId(principal);
        InventoryResponse response = updateInventoryThresholdService.execute(storeId, variantId, request.lowStockThreshold());
        return ResponseEntity.ok(response);
    }
}
