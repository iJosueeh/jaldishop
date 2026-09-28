package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.EffectiveCapacityQuery;
import com.jaldishop.backend.capacity.application.EffectiveCapacityResult;
import com.jaldishop.backend.capacity.application.GetEffectiveCapacityService;
import com.jaldishop.backend.capacity.web.dto.EffectiveCapacityResponse;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.application.StoreContextService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capacity/effective")
public class EffectiveCapacityController {

    private final StoreContextService storeContextService;
    private final GetEffectiveCapacityService getEffectiveCapacityService;

    public EffectiveCapacityController(StoreContextService storeContextService,
                                       GetEffectiveCapacityService getEffectiveCapacityService) {
        this.storeContextService = storeContextService;
        this.getEffectiveCapacityService = getEffectiveCapacityService;
    }

    @GetMapping
    public ResponseEntity<EffectiveCapacityResponse> getEffectiveCapacity(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime) {
        UUID storeId = storeContextService.requireStoreId(principal);

        EffectiveCapacityQuery query = new EffectiveCapacityQuery(storeId, date, startTime, endTime);
        EffectiveCapacityResult result = getEffectiveCapacityService.execute(query);
        return ResponseEntity.ok(EffectiveCapacityResponse.fromResult(storeId, date, startTime, endTime, result));
    }
}