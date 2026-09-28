package com.jaldishop.backend.capacity.infrastructure.persistence.adapter;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationRepository;
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
    public int countReserved(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime,
                             Instant now) {
        return (int) jpaRepository.countReserved(storeId, serviceDate, startTime, endTime, now);
    }

    @Override
    public int countCommitted(UUID storeId, LocalDate serviceDate, LocalTime startTime, LocalTime endTime) {
        return (int) jpaRepository.countCommitted(storeId, serviceDate, startTime, endTime);
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