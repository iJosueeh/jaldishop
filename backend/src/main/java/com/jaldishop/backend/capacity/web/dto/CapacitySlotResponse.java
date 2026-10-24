package com.jaldishop.backend.capacity.web.dto;

import com.jaldishop.backend.capacity.application.availability.CapacityAvailabilityResult;
import com.jaldishop.backend.capacity.application.availability.CapacitySlotAvailability;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CapacitySlotResponse(
        UUID storeId,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        int effectiveCapacity,
        int availableCapacity,
        int reservedCapacity,
        int committedCapacity,
        int occupiedCapacity
) {
    public static CapacitySlotResponse from(UUID storeId, LocalDate serviceDate, CapacitySlotAvailability slot) {
        CapacityAvailabilityResult result = slot.availability();
        // occupiedCapacity = reservas activas + capacidad comprometida (RN-CAP-07).
        // Nunca equivale solo a reservedCapacity.
        int occupied = result.reservedCapacity() + result.committedCapacity();
        return new CapacitySlotResponse(
                storeId,
                serviceDate,
                slot.startTime(),
                slot.endTime(),
                result.effectiveCapacity(),
                result.availableCapacity(),
                result.reservedCapacity(),
                result.committedCapacity(),
                occupied
        );
    }
}
