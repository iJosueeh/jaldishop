package com.jaldishop.backend.billing.web.controller;

import com.jaldishop.backend.billing.application.service.IssueFiscalReceiptService;
import com.jaldishop.backend.billing.web.dto.FiscalReceiptResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FiscalReceiptController {

    private final IssueFiscalReceiptService receiptService;
    private final StoreContextService storeContextService;

    public FiscalReceiptController(
            IssueFiscalReceiptService receiptService,
            StoreContextService storeContextService
    ) {
        this.receiptService = receiptService;
        this.storeContextService = storeContextService;
    }

    @GetMapping("/orders/{orderId}/receipt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FiscalReceiptResponse> getOrderReceipt(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        FiscalReceiptResponse receipt = receiptService.getForOrderAuthorized(orderId, principal.userId());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .body(receipt);
    }

    @GetMapping("/merchants/stores/{storeId}/billing/receipts")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<List<FiscalReceiptResponse>> listStoreReceipts(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        List<FiscalReceiptResponse> receipts = receiptService.listByStore(storeId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .body(receipts);
    }
}
