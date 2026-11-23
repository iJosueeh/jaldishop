package com.jaldishop.backend.billing.web.controller;

import com.jaldishop.backend.billing.application.service.MerchantBillingQueryService;
import com.jaldishop.backend.billing.application.service.MerchantPayoutProcessingService;
import com.jaldishop.backend.billing.web.dto.MerchantBillingSummaryResponse;
import com.jaldishop.backend.billing.web.dto.MerchantPayoutBatchResponse;
import com.jaldishop.backend.billing.web.dto.MerchantPayoutDestinationResponse;
import com.jaldishop.backend.billing.web.dto.UpdatePayoutDestinationRequest;
import com.jaldishop.backend.shared.domain.model.Money;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/billing")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
public class MerchantBillingController {

    private final StoreContextService storeContextService;
    private final MerchantBillingQueryService billingQueryService;
    private final MerchantPayoutProcessingService payoutProcessingService;

    public MerchantBillingController(
            StoreContextService storeContextService,
            MerchantBillingQueryService billingQueryService,
            MerchantPayoutProcessingService payoutProcessingService
    ) {
        this.storeContextService = storeContextService;
        this.billingQueryService = billingQueryService;
        this.payoutProcessingService = payoutProcessingService;
    }

    @GetMapping("/summary")
    public ResponseEntity<MerchantBillingSummaryResponse> getBillingSummary(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        MerchantBillingSummaryResponse summary = billingQueryService.getBillingSummary(storeId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .header("Pragma", "no-cache")
                .body(summary);
    }

    @GetMapping("/payouts")
    public ResponseEntity<List<MerchantPayoutBatchResponse>> getPayoutBatches(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        List<MerchantPayoutBatchResponse> batches = billingQueryService.getPayoutBatches(storeId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .header("Pragma", "no-cache")
                .body(batches);
    }

    @PostMapping("/payouts/request")
    public ResponseEntity<Map<String, Object>> requestPayout(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);

        MerchantPayoutDestinationResponse destination = billingQueryService.getPrimaryDestination(storeId);
        if (destination == null) {
            throw new BusinessRuleException(
                    "NO_PAYOUT_DESTINATION",
                    "Debe configurar una cuenta bancaria o Yape de destino antes de solicitar una liquidación."
            );
        }

        MerchantBillingSummaryResponse summary = billingQueryService.getBillingSummary(storeId);
        BigDecimal available = summary.availableBalance();
        BigDecimal minThreshold = summary.minPayoutThreshold();

        if (available.compareTo(minThreshold) < 0) {
            throw new BusinessRuleException(
                    "PAYOUT_BELOW_MINIMUM",
                    "El monto disponible (S/ " + available + ") es menor al umbral mínimo de retiro (S/ " + minThreshold + ")."
            );
        }

        UUID batchId = payoutProcessingService.processPayoutForStore(storeId, destination.id(), Money.pen(available));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .body(Map.of(
                        "batchId", batchId,
                        "status", "PROCESSING",
                        "amount", available,
                        "currency", "PEN",
                        "message", "Liquidación solicitada exitosamente. Se procesará en el siguiente lote bancario."
                ));
    }

    @PutMapping("/payout-destination")
    public ResponseEntity<MerchantPayoutDestinationResponse> updatePayoutDestination(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdatePayoutDestinationRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        storeContextService.validateStoreOwnership(storeId, principal);
        MerchantPayoutDestinationResponse saved = billingQueryService.saveOrUpdateDestination(storeId, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache().noStore().mustRevalidate())
                .body(saved);
    }
}
