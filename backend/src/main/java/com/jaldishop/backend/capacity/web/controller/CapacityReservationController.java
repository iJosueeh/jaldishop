package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.CapacityAvailabilityResult;
import com.jaldishop.backend.capacity.application.CreateCapacityReservationCommand;
import com.jaldishop.backend.capacity.application.CreateCapacityReservationService;
import com.jaldishop.backend.capacity.application.EffectiveCapacityQuery;
import com.jaldishop.backend.capacity.application.GetCapacityAvailabilityService;
import com.jaldishop.backend.capacity.application.GetCapacityReservationService;
import com.jaldishop.backend.capacity.application.ReleaseCapacityReservationService;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.web.dto.CapacityAvailabilityResponse;
import com.jaldishop.backend.capacity.web.dto.CapacityReservationResponse;
import com.jaldishop.backend.capacity.web.dto.CreateCapacityReservationRequest;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/capacity")
public class CapacityReservationController {

    private final GetCapacityAvailabilityService availabilityService;
    private final CreateCapacityReservationService createService;
    private final GetCapacityReservationService getService;
    private final ReleaseCapacityReservationService releaseService;

    public CapacityReservationController(GetCapacityAvailabilityService availabilityService,
                                         CreateCapacityReservationService createService,
                                         GetCapacityReservationService getService,
                                         ReleaseCapacityReservationService releaseService) {
        this.availabilityService = availabilityService;
        this.createService = createService;
        this.getService = getService;
        this.releaseService = releaseService;
    }

    @GetMapping("/availability")
    public ResponseEntity<CapacityAvailabilityResponse> getAvailability(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam(required = false) UUID storeId) {
        if (!principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden consultar la disponibilidad.");
        }

        EffectiveCapacityQuery query = new EffectiveCapacityQuery(storeId, date, startTime, endTime);
        CapacityAvailabilityResult result = availabilityService.execute(query);
        return ResponseEntity.ok(CapacityAvailabilityResponse.fromResult(storeId, date, startTime, endTime, result));
    }

    @PostMapping("/reservations")
    public ResponseEntity<CapacityReservationResponse> create(
            @AuthenticationPrincipal JwtPrincipal principal,
            @Valid @RequestBody CreateCapacityReservationRequest request) {
        if (!principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden reservar capacidad.");
        }

        CreateCapacityReservationCommand command = new CreateCapacityReservationCommand(
                request.storeId(), principal.userId(), request.serviceDate(),
                request.startTime(), request.endTime());

        CapacityReservation reservation = createService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CapacityReservationResponse.fromDomain(reservation));
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<CapacityReservationResponse> get(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        if (!principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden consultar reservas.");
        }

        CapacityReservation reservation = getService.execute(id, principal.userId());
        return ResponseEntity.ok(CapacityReservationResponse.fromDomain(reservation));
    }

    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<CapacityReservationResponse> release(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID id) {
        if (!principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden liberar reservas.");
        }

        CapacityReservation reservation = releaseService.execute(id, principal.userId());
        return ResponseEntity.ok(CapacityReservationResponse.fromDomain(reservation));
    }
}