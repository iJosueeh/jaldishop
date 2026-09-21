package com.jaldishop.backend.capacity.web.dto;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CapacityExceptionResponse(
        UUID id,
        UUID storeId,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        int exceptionCapacity,
        String reason,
        CapacityExceptionStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CapacityExceptionResponse fromDomain(CapacityException exception) {
        return new CapacityExceptionResponse(
                exception.getId(),
                exception.getStoreId(),
                exception.getServiceDate(),
                exception.getStartTime(),
                exception.getEndTime(),
                exception.getExceptionCapacity(),
                exception.getReason(),
                exception.getStatus(),
                exception.getCreatedAt(),
                exception.getUpdatedAt());
    }
}
