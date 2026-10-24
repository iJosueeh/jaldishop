package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.availability.CapacitySlotAvailability;
import com.jaldishop.backend.capacity.application.availability.ListStoreCapacitySlotsService;
import com.jaldishop.backend.capacity.web.dto.CapacitySlotResponse;
import com.jaldishop.backend.capacity.web.dto.CapacitySlotsResponse;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Consulta de franjas de capacidad de una fecha para el storefront (HU-CLI-03).
 *
 * <p>Los parámetros se leen como String y se parsean a mano: los tipos no soportados
 * (UUID/fecha mal formados o ausentes) no tienen handler en GlobalExceptionHandler y
 * caerían en un 500. Aquí se traducen al mismo 422 INVALID_CAPACITY_QUERY del motor
 * de capacidad, sin tocar la configuración de seguridad compartida.</p>
 */
@RestController
@RequestMapping("/api/v1/capacity")
public class CapacitySlotsController {

    private final ListStoreCapacitySlotsService listService;

    public CapacitySlotsController(ListStoreCapacitySlotsService listService) {
        this.listService = listService;
    }

    @GetMapping("/slots")
    public ResponseEntity<CapacitySlotsResponse> getSlots(
            @AuthenticationPrincipal JwtPrincipal principal,
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String date) {
        if (principal == null || !principal.roles().contains("CUSTOMER")) {
            throw new AccessDeniedException("Solo los usuarios con el rol CUSTOMER pueden consultar las franjas de capacidad.");
        }

        UUID parsedStoreId = parseStoreId(storeId);
        LocalDate parsedDate = parseDate(date);

        List<CapacitySlotAvailability> slots = listService.execute(parsedStoreId, parsedDate);
        List<CapacitySlotResponse> response = slots.stream()
                .map(slot -> CapacitySlotResponse.from(parsedStoreId, parsedDate, slot))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new CapacitySlotsResponse(parsedStoreId, parsedDate, response));
    }

    private UUID parseStoreId(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "El identificador de la tienda es obligatorio.");
        }
        try {
            return UUID.fromString(storeId.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "El identificador de la tienda no es válido.");
        }
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "La fecha es obligatoria para consultar las franjas.");
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("INVALID_CAPACITY_QUERY", "La fecha debe tener formato YYYY-MM-DD.");
        }
    }
}
