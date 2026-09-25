package com.jaldishop.backend.capacity.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EffectiveCapacityQuery(
        UUID storeId,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime
) {}