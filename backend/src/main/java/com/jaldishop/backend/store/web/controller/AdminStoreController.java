package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.command.ChangeStoreStatusCommand;
import com.jaldishop.backend.store.application.command.CreateStoreCommand;
import com.jaldishop.backend.store.application.query.GetAdminStoresQuery;
import com.jaldishop.backend.store.application.service.AdminCreateStoreService;
import com.jaldishop.backend.store.application.service.ChangeStoreStatusService;
import com.jaldishop.backend.store.application.service.CreateStoreService;
import com.jaldishop.backend.store.application.service.GetAdminStoresService;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.web.dto.AdminCreateStoreRequest;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
import com.jaldishop.backend.store.web.mapper.AdminStoreResponseMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminStoreController {

    private final GetAdminStoresService getAdminStoresService;
    private final ChangeStoreStatusService changeStoreStatusService;
    private final AdminCreateStoreService adminCreateStoreService;
    private final AdminStoreResponseMapper responseMapper;

    public AdminStoreController(
            GetAdminStoresService getAdminStoresService,
            ChangeStoreStatusService changeStoreStatusService,
            AdminStoreResponseMapper responseMapper
    ) {
        this(getAdminStoresService, changeStoreStatusService, null, responseMapper);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminStoreController(
            GetAdminStoresService getAdminStoresService,
            ChangeStoreStatusService changeStoreStatusService,
            AdminCreateStoreService adminCreateStoreService,
            AdminStoreResponseMapper responseMapper
    ) {
        this.getAdminStoresService = getAdminStoresService;
        this.changeStoreStatusService = changeStoreStatusService;
        this.adminCreateStoreService = adminCreateStoreService;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public ResponseEntity<List<AdminStoreSummaryResponse>> listStores(
            @RequestParam(required = false)
            @Size(max = 100, message = "El texto de búsqueda no puede exceder 100 caracteres") String query,
            @RequestParam(required = false) StoreStatus status
    ) {
        List<Store> stores = getAdminStoresService.execute(new GetAdminStoresQuery(query, status));
        List<AdminStoreSummaryResponse> response = stores.stream()
                .map(responseMapper::toSummary)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminStoreDetailResponse> getStoreById(@PathVariable UUID id) {
        Store store = getAdminStoresService.execute(id);
        return ResponseEntity.ok(responseMapper.toDetail(store));
    }

    @PostMapping
    public ResponseEntity<AdminStoreDetailResponse> createStore(
            @Valid @RequestBody AdminCreateStoreRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        CreateStoreCommand command = new CreateStoreCommand(
                request.merchantUserId(),
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

        UUID adminId = principal != null ? principal.userId() : null;
        Store store = adminCreateStoreService.execute(command, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toDetail(store));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AdminStoreDetailResponse> suspendStore(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        Store store = changeStoreStatusService.execute(new ChangeStoreStatusCommand(id, StoreStatus.SUSPENDED, adminId));
        return ResponseEntity.ok(responseMapper.toDetail(store));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AdminStoreDetailResponse> activateStore(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        Store store = changeStoreStatusService.execute(new ChangeStoreStatusCommand(id, StoreStatus.ACTIVE, adminId));
        return ResponseEntity.ok(responseMapper.toDetail(store));
    }
}
