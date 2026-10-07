package com.jaldishop.backend.inventory.infrastructure.persistence.adapter;

import com.jaldishop.backend.inventory.domain.InventoryReservation;
import com.jaldishop.backend.inventory.domain.InventoryReservationRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryReservationEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.mapper.InventoryReservationPersistenceMapper;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryReservationJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InventoryReservationRepositoryAdapter implements InventoryReservationRepository {

    private final InventoryReservationJpaRepository jpaRepository;
    private final InventoryReservationPersistenceMapper mapper;

    public InventoryReservationRepositoryAdapter(
            InventoryReservationJpaRepository jpaRepository,
            InventoryReservationPersistenceMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public InventoryReservation save(InventoryReservation reservation) {
        InventoryReservationEntity entity = mapper.toEntity(reservation);
        InventoryReservationEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<InventoryReservation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<InventoryReservation> findByCapacityReservationIdAndVariantId(UUID capacityReservationId, UUID variantId) {
        return jpaRepository.findByCapacityReservationIdAndVariantId(capacityReservationId, variantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<InventoryReservation> findByCapacityReservationId(UUID capacityReservationId) {
        return jpaRepository.findByCapacityReservationId(capacityReservationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<InventoryReservation> findExpiredActiveReservations(Instant now) {
        return jpaRepository.findByStatusAndExpiresAtBefore("ACTIVE", now).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
