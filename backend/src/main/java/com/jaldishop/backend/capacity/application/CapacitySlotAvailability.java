package com.jaldishop.backend.capacity.application;

import java.time.LocalTime;

public record CapacitySlotAvailability(
        LocalTime startTime,
        LocalTime endTime,
        CapacityAvailabilityResult availability
) {}
