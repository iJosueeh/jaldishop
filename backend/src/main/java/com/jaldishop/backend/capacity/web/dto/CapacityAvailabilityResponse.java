package com.jaldishop.backend.capacity.web.dto;

import com.jaldishop.backend.capacity.application.CapacityAvailabilityResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CapacityAvailabilityResponse(
        UUID storeId,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        int effectiveCapacity,
        int availableCapacity,
        int reservedCapacity,
        int committedCapacity
) {
    public static CapacityAvailabilityResponse fromResult(UUID storeId, LocalDate serviceDate,
                                                          LocalTime startTime, LocalTime endTime,
                                                          CapacityAvailabilityResult result) {
        return new CapacityAvailabilityResponse(
                storeId, serviceDate, startTime, endTime,
                result.effectiveCapacity(), result.availableCapacity(),
                result.reservedCapacity(), result.committedCapacity());
    }
}