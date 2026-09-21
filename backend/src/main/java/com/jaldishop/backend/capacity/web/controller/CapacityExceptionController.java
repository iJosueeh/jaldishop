package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.*;
import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.web.dto.CapacityExceptionResponse;
import com.jaldishop.backend.capacity.web.dto.CreateCapacityExceptionRequest;
import com.jaldishop.backend.capacity.web.dto.UpdateCapacityExceptionRequest;
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
@RequestMapping("/api/v1/capacity-exceptions")
public class CapacityExceptionController {

    private final GetMyStoreService getMyStoreService;
    private final CreateCapacityExceptionService createService;
    private final GetStoreCapacityExceptionsService listService;
    private final UpdateCapacityExceptionService updateService;
    private final ToggleCapacityExceptionStatusService toggleService;

    public CapacityExceptionController(GetMyStoreService getMyStoreService,
                                       CreateCapacityExceptionService createService,
                                       GetStoreCapacityExceptionsService listService,
                                       UpdateCapacityExceptionService updateService,
                                       ToggleCapacityExceptionStatusService toggleService) {
        this.getMyStoreService = getMyStoreService;
        this.createService = createService;
        this.listService = listService;
        this.updateService = updateService;
        this.toggleService = toggleService;
    }

    @PostMapping
    public ResponseEntity<CapacityExceptionResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCapacityExceptionRequest request) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        CreateCapacityExceptionCommand command = new CreateCapacityExceptionCommand(
                storeId, request.serviceDate(), request.startTime(), request.endTime(),
                request.exceptionCapacity(), request.reason());

        CapacityException exception = createService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CapacityExceptionResponse.fromDomain(exception));
    }

    @GetMapping
    public ResponseEntity<List<CapacityExceptionResponse>> list(
            @AuthenticationPrincipal JwtPrincipal principal) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        List<CapacityException> exceptions = listService.execute(storeId);
        List<CapacityExceptionResponse> response = exceptions.stream()
                .map(CapacityExceptionResponse::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CapacityExceptionResponse> update(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCapacityExceptionRequest request) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        UpdateCapacityExceptionCommand command = new UpdateCapacityExceptionCommand(
                id, storeId, request.serviceDate(), request.startTime(), request.endTime(),
                request.exceptionCapacity(), request.reason());

        CapacityException exception = updateService.execute(command);
        return ResponseEntity.ok(CapacityExceptionResponse.fromDomain(exception));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<CapacityExceptionResponse> activate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        CapacityException exception = toggleService.activate(id, storeId);
        return ResponseEntity.ok(CapacityExceptionResponse.fromDomain(exception));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CapacityExceptionResponse> deactivate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        requireMerchantRole(principal);
        UUID storeId = resolveStoreId(principal);

        CapacityException exception = toggleService.deactivate(id, storeId);
        return ResponseEntity.ok(CapacityExceptionResponse.fromDomain(exception));
    }

    private UUID resolveStoreId(JwtPrincipal principal) {
        Store store = getMyStoreService.execute(principal.userId());
        return store.getId();
    }

    private void requireMerchantRole(JwtPrincipal principal) {
        if (!principal.roles().contains("MERCHANT")) {
            throw new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden gestionar excepciones de capacidad.");
        }
    }
}
