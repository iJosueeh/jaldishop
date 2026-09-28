package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.store.application.ChangeStoreStatusCommand;
import com.jaldishop.backend.store.application.ChangeStoreStatusService;
import com.jaldishop.backend.store.application.GetAdminStoresQuery;
import com.jaldishop.backend.store.application.GetAdminStoresService;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
import com.jaldishop.backend.store.web.mapper.AdminStoreResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {

    private final GetAdminStoresService getAdminStoresService;
    private final ChangeStoreStatusService changeStoreStatusService;
    private final AdminStoreResponseMapper responseMapper;

    public AdminStoreController(
            GetAdminStoresService getAdminStoresService,
            ChangeStoreStatusService changeStoreStatusService,
            AdminStoreResponseMapper responseMapper
    ) {
        this.getAdminStoresService = getAdminStoresService;
        this.changeStoreStatusService = changeStoreStatusService;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public ResponseEntity<List<AdminStoreSummaryResponse>> listStores(
            @RequestParam(required = false) String query,
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

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AdminStoreDetailResponse> suspendStore(@PathVariable UUID id) {
        Store store = changeStoreStatusService.execute(new ChangeStoreStatusCommand(id, StoreStatus.SUSPENDED));
        return ResponseEntity.ok(responseMapper.toDetail(store));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AdminStoreDetailResponse> activateStore(@PathVariable UUID id) {
        Store store = changeStoreStatusService.execute(new ChangeStoreStatusCommand(id, StoreStatus.ACTIVE));
        return ResponseEntity.ok(responseMapper.toDetail(store));
    }
}
