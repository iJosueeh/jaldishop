package com.jaldishop.backend.capacity.application;

import java.time.LocalTime;
import java.util.UUID;

public record UpdateCapacityConfigurationCommand(
        UUID configId,
        UUID storeId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity
) {}
