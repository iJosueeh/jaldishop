package com.jaldishop.backend.capacity.application.availability;

import java.time.LocalTime;

public record CapacitySlotAvailability(
        LocalTime startTime,
        LocalTime endTime,
        CapacityAvailabilityResult availability
) {}
