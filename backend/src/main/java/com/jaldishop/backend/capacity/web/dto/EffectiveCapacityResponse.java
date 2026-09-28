package com.jaldishop.backend.capacity.web.dto;

import com.jaldishop.backend.capacity.application.EffectiveCapacityResult;
import com.jaldishop.backend.capacity.application.EffectiveCapacitySource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EffectiveCapacityResponse(
        UUID storeId,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        int effectiveCapacity,
        EffectiveCapacitySource source
) {
    public static EffectiveCapacityResponse fromResult(UUID storeId, LocalDate serviceDate,
                                                       LocalTime startTime, LocalTime endTime,
                                                       EffectiveCapacityResult result) {
        return new EffectiveCapacityResponse(
                storeId, serviceDate, startTime, endTime, result.capacity(), result.source());
    }
}