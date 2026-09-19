package com.jaldishop.backend.capacity.web.dto;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationStatus;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record CapacityConfigurationResponse(
        UUID id,
        UUID storeId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity,
        CapacityConfigurationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CapacityConfigurationResponse fromDomain(CapacityConfiguration config) {
        return new CapacityConfigurationResponse(
                config.getId(),
                config.getStoreId(),
                config.getDayOfWeek(),
                config.getStartTime(),
                config.getEndTime(),
                config.getMaxCapacity(),
                config.getStatus(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }
}
