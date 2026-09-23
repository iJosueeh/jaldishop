package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.store.application.ChangeStoreStatusService;
import com.jaldishop.backend.store.application.GetAdminStoresService;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final GetAdminStoresService getAdminStoresService;
    private final ChangeStoreStatusService changeStoreStatusService;

    public AdminStoreController(GetAdminStoresService getAdminStoresService, ChangeStoreStatusService changeStoreStatusService) {
        this.getAdminStoresService = getAdminStoresService;
        this.changeStoreStatusService = changeStoreStatusService;
    }

    @GetMapping
    public ResponseEntity<List<AdminStoreSummaryResponse>> listStores(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) StoreStatus status
    ) {
        List<AdminStoreSummaryResponse> stores = getAdminStoresService.listStores(query, status);
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminStoreDetailResponse> getStoreById(@PathVariable UUID id) {
        AdminStoreDetailResponse store = getAdminStoresService.getStoreById(id);
        return ResponseEntity.ok(store);
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AdminStoreDetailResponse> suspendStore(@PathVariable UUID id) {
        AdminStoreDetailResponse store = changeStoreStatusService.suspend(id);
        return ResponseEntity.ok(store);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AdminStoreDetailResponse> activateStore(@PathVariable UUID id) {
        AdminStoreDetailResponse store = changeStoreStatusService.activate(id);
        return ResponseEntity.ok(store);
    }
}
