package com.jaldishop.backend.capacity.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateCapacityExceptionRequest(
        @NotNull LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        @Min(0) int exceptionCapacity,
        String reason
) {}
