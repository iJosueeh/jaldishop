package com.jaldishop.backend.capacity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface CapacityReservationRepository {
    Optional<CapacityReservation> findById(UUID id);
    int countReserved(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime, Instant now);
    int countCommitted(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime);
    boolean existsReservedByUserAndWindow(UUID userId, UUID storeId, LocalDate serviceDate,
                                          LocalTime startTime, LocalTime endTime, Instant now);
    CapacityReservation save(CapacityReservation reservation);
}