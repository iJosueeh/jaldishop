package com.jaldishop.backend.inventory.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository {
    InventoryReservation save(InventoryReservation reservation);
    Optional<InventoryReservation> findById(UUID id);
    Optional<InventoryReservation> findByCapacityReservationIdAndVariantId(UUID capacityReservationId, UUID variantId);
    List<InventoryReservation> findByCapacityReservationId(UUID capacityReservationId);
    List<InventoryReservation> findExpiredActiveReservations(Instant now);
}
