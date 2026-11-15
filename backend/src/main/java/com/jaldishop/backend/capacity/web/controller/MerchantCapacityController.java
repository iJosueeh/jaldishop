package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.occupancy.service.GetMerchantStoreCapacityOccupancyService;
import com.jaldishop.backend.capacity.web.dto.MerchantStoreCapacityOccupancyResponse;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controlador REST para la consulta de capacidad y ocupación operativa real por parte del comerciante.
 *
 * <p><strong>Ruta Base:</strong> {@code /api/v1/merchants/stores/{storeId}/capacity}</p>
 * <p><strong>Seguridad:</strong>
 * Protegido con {@code @PreAuthorize("hasRole('MERCHANT')")}.
 * Valida la pertenencia de la tienda mediante {@link StoreContextService#validateStoreOwnership(UUID, JwtPrincipal)}.</p>
 */
@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/capacity")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
public class MerchantCapacityController {

    private final StoreContextService storeContextService;
    private final GetMerchantStoreCapacityOccupancyService occupancyService;

    public MerchantCapacityController(
            StoreContextService storeContextService,
            GetMerchantStoreCapacityOccupancyService occupancyService
    ) {
        this.storeContextService = storeContextService;
        this.occupancyService = occupancyService;
    }

    @GetMapping("/occupancy")
    public ResponseEntity<MerchantStoreCapacityOccupancyResponse> getOccupancy(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        if (principal == null) {
            throw new BadCredentialsException("Autenticación requerida");
        }
        storeContextService.validateStoreOwnership(storeId, principal);
        MerchantStoreCapacityOccupancyResponse response = occupancyService.execute(storeId, date);
        return ResponseEntity.ok(response);
    }
}
