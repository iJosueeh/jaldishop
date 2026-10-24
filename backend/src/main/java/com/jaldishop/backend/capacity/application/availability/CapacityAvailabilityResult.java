package com.jaldishop.backend.capacity.application.availability;

public record CapacityAvailabilityResult(
        int effectiveCapacity,
        int availableCapacity,
        int reservedCapacity,
        int committedCapacity
) {}