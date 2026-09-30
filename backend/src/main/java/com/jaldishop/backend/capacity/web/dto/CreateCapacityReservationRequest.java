package com.jaldishop.backend.capacity.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateCapacityReservationRequest(
        @NotNull UUID storeId,
        @NotNull LocalDate serviceDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}