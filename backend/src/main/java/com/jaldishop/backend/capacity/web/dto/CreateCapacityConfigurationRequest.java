package com.jaldishop.backend.capacity.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalTime;

public record CreateCapacityConfigurationRequest(
        @Min(0) @Max(6) int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        @Min(0) int maxCapacity
) {}
