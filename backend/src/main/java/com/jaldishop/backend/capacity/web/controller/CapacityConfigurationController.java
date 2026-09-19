package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.*;
import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.web.dto.CapacityConfigurationResponse;
import com.jaldishop.backend.capacity.web.dto.CreateCapacityConfigurationRequest;
import com.jaldishop.backend.capacity.web.dto.UpdateCapacityConfigurationRequest;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.GetMyStoreService;
import com.jaldishop.backend.store.domain.Store;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/capacity-configurations")
public class CapacityConfigurationController {

    private final GetMyStoreService getMyStoreService;
    private final CreateCapacityConfigurationService createService;
    private final GetStoreCapacityConfigurationsService listService;
    private final UpdateCapacityConfigurationService updateService;
    private final ToggleCapacityConfigurationStatusService toggleService;

    public CapacityConfigurationController(GetMyStoreService getMyStoreService,
                                           CreateCapacityConfigurationService createService,
                                           GetStoreCapacityConfigurationsService listService,
                                           UpdateCapacityConfigurationService updateService,
                                           ToggleCapacityConfigurationStatusService toggleService) {
        this.getMyStoreService = getMyStoreService;
        this.createService = createService;
        this.listService = listService;
        this.updateService = updateService;
        this.toggleService = toggleService;
    }

    @PostMapping
    public ResponseEntity<CapacityConfigurationResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCapacityConfigurationRequest request) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        CreateCapacityConfigurationCommand command = new CreateCapacityConfigurationCommand(
                storeId, request.dayOfWeek(), request.startTime(), request.endTime(), request.maxCapacity());

        CapacityConfiguration config = createService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CapacityConfigurationResponse.fromDomain(config));
    }

    @GetMapping
    public ResponseEntity<List<CapacityConfigurationResponse>> list(
            @AuthenticationPrincipal JwtPrincipal principal) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        List<CapacityConfiguration> configs = listService.execute(storeId);
        List<CapacityConfigurationResponse> response = configs.stream()
                .map(CapacityConfigurationResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CapacityConfigurationResponse> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCapacityConfigurationRequest request) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        UpdateCapacityConfigurationCommand command = new UpdateCapacityConfigurationCommand(
                id, storeId, request.dayOfWeek(), request.startTime(), request.endTime(), request.maxCapacity());

        CapacityConfiguration config = updateService.execute(command);
        return ResponseEntity.ok(CapacityConfigurationResponse.fromDomain(config));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CapacityConfigurationResponse> activate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        CapacityConfiguration config = toggleService.activate(id, storeId);
        return ResponseEntity.ok(CapacityConfigurationResponse.fromDomain(config));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CapacityConfigurationResponse> deactivate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        CapacityConfiguration config = toggleService.deactivate(id, storeId);
        return ResponseEntity.ok(CapacityConfigurationResponse.fromDomain(config));
    }

    private UUID resolveStoreId(JwtPrincipal principal) {
        Store store = getMyStoreService.execute(principal.userId());
        return store.getId();
    }

    private void requireMerchantRole(JwtPrincipal principal) {
        if (!principal.roles().contains("MERCHANT")) {
            throw new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden gestionar capacidad.");
        }
    }
}
