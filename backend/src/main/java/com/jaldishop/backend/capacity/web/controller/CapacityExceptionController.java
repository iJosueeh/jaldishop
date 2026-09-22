package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.*;
import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.web.dto.CapacityExceptionResponse;
import com.jaldishop.backend.capacity.web.dto.CreateCapacityExceptionRequest;
import com.jaldishop.backend.capacity.web.dto.UpdateCapacityExceptionRequest;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.StoreContextService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/capacity-exceptions")
public class CapacityExceptionController {

    private final StoreContextService storeContextService;
    private final CreateCapacityExceptionService createService;
    private final GetStoreCapacityExceptionsService listService;
    private final UpdateCapacityExceptionService updateService;
    private final ToggleCapacityExceptionStatusService toggleService;

    public CapacityExceptionController(StoreContextService storeContextService,
                                       CreateCapacityExceptionService createService,
                                       GetStoreCapacityExceptionsService listService,
                                       UpdateCapacityExceptionService updateService,
                                       ToggleCapacityExceptionStatusService toggleService) {
        this.storeContextService = storeContextService;
        this.createService = createService;
        this.listService = listService;
        this.updateService = updateService;
        this.toggleService = toggleService;
    }

    @PostMapping
    public ResponseEntity<CapacityExceptionResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCapacityExceptionRequest request) {
        UUID storeId = storeContextService.requireStoreId(principal);

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
        UUID storeId = storeContextService.requireStoreId(principal);

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
        UUID storeId = storeContextService.requireStoreId(principal);

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
        UUID storeId = storeContextService.requireStoreId(principal);

        CapacityException exception = toggleService.activate(id, storeId);
        return ResponseEntity.ok(CapacityExceptionResponse.fromDomain(exception));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CapacityExceptionResponse> deactivate(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        UUID storeId = storeContextService.requireStoreId(principal);

        CapacityException exception = toggleService.deactivate(id, storeId);
        return ResponseEntity.ok(CapacityExceptionResponse.fromDomain(exception));
    }
}