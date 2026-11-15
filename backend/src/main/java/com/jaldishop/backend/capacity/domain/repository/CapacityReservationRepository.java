package com.jaldishop.backend.capacity.domain.repository;

import com.jaldishop.backend.capacity.domain.model.*;
import com.jaldishop.backend.capacity.domain.enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface CapacityReservationRepository {
    Optional<CapacityReservation> findById(UUID id);
    Optional<CapacityReservation> findByIdForUpdate(UUID id);
    int countReserved(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime, Instant now);
    int countCommitted(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime);
    int countDailyReserved(UUID storeId, LocalDate serviceDate, Instant now);
    int countDailyCommitted(UUID storeId, LocalDate serviceDate);
    java.util.List<CapacityPeriod> findOccupiedWindows(UUID storeId, LocalDate serviceDate, Instant now);
    boolean existsReservedByUserAndWindow(UUID userId, UUID storeId, LocalDate serviceDate,
                                          LocalTime startTime, LocalTime endTime, Instant now);
    CapacityReservation save(CapacityReservation reservation);
}