package com.jaldishop.backend.capacity.infrastructure.persistence.adapter;

import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityReservationEntity;
import com.jaldishop.backend.capacity.infrastructure.persistence.mapper.CapacityReservationPersistenceMapper;
import com.jaldishop.backend.capacity.infrastructure.persistence.repository.CapacityReservationJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CapacityReservationRepositoryAdapter implements CapacityReservationRepository {

    private final CapacityReservationJpaRepository jpaRepository;
    private final CapacityReservationPersistenceMapper mapper;

    public CapacityReservationRepositoryAdapter(CapacityReservationJpaRepository jpaRepository,
                                                CapacityReservationPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<CapacityReservation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<CapacityReservation> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public int countReserved(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime,
                             Instant now) {
        return (int) jpaRepository.countReserved(storeId, serviceDate, startTime, endTime, now);
    }

    @Override
    public int countCommitted(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime) {
        return (int) jpaRepository.countCommitted(storeId, serviceDate, startTime, endTime);
    }

    @Override
    public int countDailyReserved(UUID storeId, LocalDate serviceDate, Instant now) {
        return (int) jpaRepository.countDailyReserved(storeId, serviceDate, now);
    }

    @Override
    public int countDailyCommitted(UUID storeId, LocalDate serviceDate) {
        return (int) jpaRepository.countDailyCommitted(storeId, serviceDate);
    }

    @Override
    public java.util.List<com.jaldishop.backend.capacity.domain.model.CapacityPeriod> findOccupiedWindows(
            UUID storeId, LocalDate serviceDate, Instant now) {
        java.util.List<Object[]> rows = jpaRepository.findOccupiedTimeRanges(storeId, serviceDate, now);
        java.util.List<com.jaldishop.backend.capacity.domain.model.CapacityPeriod> windows = new java.util.ArrayList<>(rows.size());
        for (Object[] row : rows) {
            LocalTime start = (LocalTime) row[0];
            LocalTime end = (LocalTime) row[1];
            windows.add(com.jaldishop.backend.capacity.domain.model.CapacityPeriod.of(start, end));
        }
        return windows;
    }

    @Override
    public boolean existsReservedByUserAndWindow(UUID userId, UUID storeId, LocalDate serviceDate,
                                                 LocalTime startTime, LocalTime endTime, Instant now) {
        return jpaRepository.existsReservedByUserAndWindow(userId, storeId, serviceDate, startTime, endTime, now);
    }

    @Override
    public CapacityReservation save(CapacityReservation reservation) {
        CapacityReservationEntity entity = mapper.toEntity(reservation);
        CapacityReservationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
}