package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.store.application.command.*;
import com.jaldishop.backend.store.application.result.*;
import com.jaldishop.backend.store.application.service.*;

import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.domain.repository.StoreSettingsRepository;
import com.jaldishop.backend.store.web.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stores")
@Validated
public class StoreController {

    private final CreateStoreService createStoreService;
    private final GetMyStoreService getMyStoreService;
    private final UpdateStoreService updateStoreService;
    private final GetStoreBySlugService getStoreBySlugService;
    private final SearchPublicStoresService searchPublicStoresService;
    private final CloseMyStoreService closeMyStoreService;
    private final com.jaldishop.backend.store.application.service.UpdateStoreTaxProfileService updateStoreTaxProfileService;
    private StoreSettingsRepository storeSettingsRepository;

    @Autowired(required = false)
    public void setStoreSettingsRepository(StoreSettingsRepository storeSettingsRepository) {
        this.storeSettingsRepository = storeSettingsRepository;
    }

    public StoreController(
            CreateStoreService createStoreService,
            GetMyStoreService getMyStoreService,
            UpdateStoreService updateStoreService,
            GetStoreBySlugService getStoreBySlugService,
            SearchPublicStoresService searchPublicStoresService,
            CloseMyStoreService closeMyStoreService
    ) {
        this(createStoreService, getMyStoreService, updateStoreService, getStoreBySlugService, searchPublicStoresService, closeMyStoreService, null);
    }

    @Autowired
    public StoreController(
            CreateStoreService createStoreService,
            GetMyStoreService getMyStoreService,
            UpdateStoreService updateStoreService,
            GetStoreBySlugService getStoreBySlugService,
            SearchPublicStoresService searchPublicStoresService,
            CloseMyStoreService closeMyStoreService,
            com.jaldishop.backend.store.application.service.UpdateStoreTaxProfileService updateStoreTaxProfileService
    ) {
        this.createStoreService = createStoreService;
        this.getMyStoreService = getMyStoreService;
        this.updateStoreService = updateStoreService;
        this.getStoreBySlugService = getStoreBySlugService;
        this.searchPublicStoresService = searchPublicStoresService;
        this.closeMyStoreService = closeMyStoreService;
        this.updateStoreTaxProfileService = updateStoreTaxProfileService;
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
                request.categoryIds(),
                request.legalBusinessName(),
                request.taxIdType(),
                request.taxIdNumber(),
                request.taxRegime(),
                request.canIssueInvoices(),
                Boolean.TRUE.equals(request.sunatTermsAccepted()) ? java.time.Instant.now() : null
        );

        Store store = createStoreService.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                StoreResponse.fromDomain(store)
        );
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<StoreResponse> getMyStore(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        Store store = getMyStoreService.execute(principal.userId());
        return ResponseEntity.ok(StoreResponse.fromDomain(store));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
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

    @PutMapping("/me/tax-profile")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<StoreResponse> updateTaxProfile(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody com.jaldishop.backend.store.web.dto.UpdateStoreTaxProfileRequest request
    ) {
        Store store = updateStoreTaxProfileService.execute(principal.userId(), request);
        return ResponseEntity.ok(StoreResponse.fromDomain(store));
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<CloseStoreResponse> closeMyStore(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody(required = false) CloseStoreRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        CloseStoreResult result = closeMyStoreService.execute(new CloseMyStoreCommand(principal.userId(), reason));
        return ResponseEntity.ok(CloseStoreResponse.fromResult(result));
    }

    @PatchMapping("/me/deactivate")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<CloseStoreResponse> deactivateMyStore(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        CloseStoreResult result = closeMyStoreService.deactivate(principal.userId());
        return ResponseEntity.ok(CloseStoreResponse.fromResult(result));
    }

    @PatchMapping("/me/reactivate")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<StoreResponse> reactivateMyStore(
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        Store store = closeMyStoreService.reactivate(principal.userId());
        return ResponseEntity.ok(StoreResponse.fromDomain(store));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PublicStoreResponse> getStoreBySlug(
            @PathVariable @Size(max = 180, message = "El slug no puede exceder 180 caracteres") String slug
    ) {
        Store store = getStoreBySlugService.execute(slug);
        return ResponseEntity.ok(PublicStoreResponse.fromDomain(store));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PublicStoreResponse>> searchPublicStores(
            @RequestParam(required = false, defaultValue = "")
            @Size(max = 100, message = "El texto de búsqueda no puede exceder 100 caracteres") String q,
            @RequestParam(required = false, defaultValue = "5")
            @Min(value = 1, message = "El límite debe ser al menos 1")
            @Max(value = 50, message = "El límite no puede exceder 50") int limit
    ) {
        List<PublicStoreResponse> stores = searchPublicStoresService.execute(q).stream()
                .limit(limit)
                .map(PublicStoreResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<PublicStoreResponse>> getFeaturedStores(
            @RequestParam(required = false, defaultValue = "6")
            @Min(value = 1, message = "El límite debe ser al menos 1")
            @Max(value = 50, message = "El límite no puede exceder 50") int limit
    ) {
        List<PublicStoreResponse> stores = searchPublicStoresService.execute("").stream()
                .limit(limit)
                .map(PublicStoreResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{storeId}/public-settings")
    public ResponseEntity<PublicStoreSettingsResponse> getPublicStoreSettings(
            @PathVariable UUID storeId
    ) {
        StoreSettings settings = storeSettingsRepository != null
                ? storeSettingsRepository.findByStoreId(storeId).orElseGet(() -> StoreSettings.defaultSettings(storeId))
                : StoreSettings.defaultSettings(storeId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePublic())
                .body(PublicStoreSettingsResponse.fromDomain(settings));
    }

}
