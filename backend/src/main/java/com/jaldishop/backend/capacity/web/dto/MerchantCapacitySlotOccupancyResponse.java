package com.jaldishop.backend.capacity.web.dto;

import java.time.LocalTime;

public record MerchantCapacitySlotOccupancyResponse(
        LocalTime startTime,
        LocalTime endTime,
        int effectiveCapacity,
        int occupiedCapacity,
        int availableCapacity,
        int reservedCapacity,
        int committedCapacity,
        int occupancyPercentage
) {}
