package com.jaldishop.backend.capacity.web.dto;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CapacityReservationResponse(
        UUID id,
        UUID storeId,
        UUID userId,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        CapacityReservationStatus status,
        Instant expiresAt,
        Instant paymentProtectionExpiresAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static CapacityReservationResponse fromDomain(CapacityReservation reservation) {
        return new CapacityReservationResponse(
                reservation.getId(),
                reservation.getStoreId(),
                reservation.getUserId(),
                reservation.getServiceDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                reservation.getPaymentProtectionExpiresAt(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt());
    }
}