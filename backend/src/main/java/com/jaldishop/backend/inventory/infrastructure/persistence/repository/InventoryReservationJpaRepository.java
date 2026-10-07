package com.jaldishop.backend.inventory.infrastructure.persistence.repository;

import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryReservationJpaRepository extends JpaRepository<InventoryReservationEntity, UUID> {

    Optional<InventoryReservationEntity> findByCapacityReservationIdAndVariantId(UUID capacityReservationId, UUID variantId);

    List<InventoryReservationEntity> findByCapacityReservationId(UUID capacityReservationId);

    List<InventoryReservationEntity> findByStatusAndExpiresAtBefore(String status, Instant expiresAt);
}
