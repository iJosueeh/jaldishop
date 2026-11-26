package com.jaldishop.backend.promo.web.controller;

import com.jaldishop.backend.promo.application.dto.PromoCodeResponse;
import com.jaldishop.backend.promo.application.service.ManageAdminPromoCodeService;
import com.jaldishop.backend.promo.web.dto.TogglePromoCodeRequest;
import com.jaldishop.backend.promo.web.dto.UpsertPromoCodeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/promo-codes")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromoCodeController {

    private final ManageAdminPromoCodeService service;

    public AdminPromoCodeController(ManageAdminPromoCodeService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PromoCodeResponse>> listGlobal() {
        return ResponseEntity.ok(service.listAllGlobal());
    }

    @PostMapping
    public ResponseEntity<PromoCodeResponse> create(@Valid @RequestBody UpsertPromoCodeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromoCodeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertPromoCodeRequest request
    ) {
        return ResponseEntity.ok(service.update(id, request.toCommand()));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<PromoCodeResponse> toggle(
            @PathVariable UUID id,
            @Valid @RequestBody TogglePromoCodeRequest request
    ) {
        return ResponseEntity.ok(service.toggleActive(id, request.active()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
