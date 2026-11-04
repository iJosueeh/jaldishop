package com.jaldishop.backend.capacity.application.availability.result;

public record CapacityAvailabilityResult(
        int effectiveCapacity,
        int availableCapacity,
        int reservedCapacity,
        int committedCapacity
) {}