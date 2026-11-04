package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.inventory.domain.enums.InventoryReservationStatus;
import com.jaldishop.backend.inventory.domain.model.InventoryReservation;
import com.jaldishop.backend.inventory.domain.repository.InventoryReservationRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

/**
 * Restituye el inventario descontado de una orden cancelada tempranamente (en estado CONFIRMED).
 * Idempotente: solo procesa reservas en estado COMMITTED y las transiciona a RELEASED.
 */
@Service
@Transactional
public class RestockInventoryService {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public RestockInventoryService(
            InventoryJpaRepository inventoryJpaRepository,
            InventoryReservationRepository inventoryReservationRepository
    ) {
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
    }

    public void execute(UUID storeId, UUID capacityReservationId) {
        if (capacityReservationId == null) {
            return;
        }

        List<InventoryReservation> reservations = inventoryReservationRepository
                .findByCapacityReservationId(capacityReservationId);

        List<InventoryReservation> committedReservations = reservations.stream()
                .filter(r -> r.getStatus() == InventoryReservationStatus.COMMITTED)
                .toList();

        if (committedReservations.isEmpty()) {
            return;
        }

        var quantitiesByVariant = committedReservations.stream()
                .collect(groupingBy(
                        InventoryReservation::getVariantId,
                        summingInt(InventoryReservation::getQuantity)
                ));

        List<UUID> sortedVariantIds = quantitiesByVariant.keySet().stream()
                .sorted()
                .toList();

        // Lock pesimista en orden canónico variant_id ASC
        List<InventoryEntity> inventories = inventoryJpaRepository.findByVariantIdInForUpdate(sortedVariantIds);
        Map<UUID, InventoryEntity> inventoryMap = inventories.stream()
                .collect(Collectors.toMap(InventoryEntity::getVariantId, Function.identity()));

        Instant now = Instant.now();

        // Restituir existencias físicas
        for (UUID variantId : sortedVariantIds) {
            int quantityToRestore = quantitiesByVariant.get(variantId);
            InventoryEntity inventory = inventoryMap.get(variantId);
            if (inventory != null) {
                inventory.setQuantity(inventory.getQuantity() + quantityToRestore);
                inventory.setUpdatedAt(now);
                inventoryJpaRepository.save(inventory);
            }
        }

        // Marcar las reservas como RELEASED
        for (InventoryReservation reservation : committedReservations) {
            reservation.releaseFromCancelledOrder();
            inventoryReservationRepository.save(reservation);
        }
    }
}
