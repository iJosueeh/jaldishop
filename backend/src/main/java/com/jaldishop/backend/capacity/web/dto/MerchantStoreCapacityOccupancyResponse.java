package com.jaldishop.backend.capacity.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record MerchantStoreCapacityOccupancyResponse(
        UUID storeId,
        LocalDate date,
        boolean isClosed,
        int effectiveCapacity,
        int occupiedCapacity,
        int availableCapacity,
        int reservedCapacity,
        int committedCapacity,
        int occupancyPercentage,
        List<MerchantCapacitySlotOccupancyResponse> slots
) {}
