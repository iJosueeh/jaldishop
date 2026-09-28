package com.jaldishop.backend.capacity.application;

public record CapacityAvailabilityResult(
        int effectiveCapacity,
        int availableCapacity,
        int reservedCapacity,
        int committedCapacity
) {}