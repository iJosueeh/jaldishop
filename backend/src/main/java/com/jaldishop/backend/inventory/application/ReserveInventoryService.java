package com.jaldishop.backend.inventory.application;

import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.inventory.domain.InventoryReservation;
import com.jaldishop.backend.inventory.domain.InventoryReservationRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReserveInventoryService {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public ReserveInventoryService(
            InventoryJpaRepository inventoryJpaRepository,
            InventoryReservationRepository inventoryReservationRepository
    ) {
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
    }

    public List<InventoryReservation> execute(
            UUID storeId,
            UUID capacityReservationId,
            Instant expiresAt,
            List<CheckoutItemSnapshot> items
    ) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, Integer> requestedQuantities = items.stream()
                .filter(CheckoutItemSnapshot::tracksInventory)
                .collect(Collectors.groupingBy(
                        CheckoutItemSnapshot::variantId,
                        Collectors.summingInt(CheckoutItemSnapshot::quantity)
                ));

        if (requestedQuantities.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> sortedVariantIds = requestedQuantities.keySet().stream()
                .sorted()
                .toList();

        List<InventoryEntity> lockedInventories = inventoryJpaRepository.findByVariantIdInForUpdate(sortedVariantIds);

        Set<UUID> foundVariantIds = lockedInventories.stream()
                .map(InventoryEntity::getVariantId)
                .collect(Collectors.toSet());

        for (UUID variantId : sortedVariantIds) {
            if (!foundVariantIds.contains(variantId)) {
                throw new ConflictException(
                        "INVENTORY_NOT_CONFIGURED",
                        "El inventario no está configurado para la variante con control de stock: " + variantId
                );
            }
        }

        Map<UUID, InventoryEntity> inventoryMap = lockedInventories.stream()
                .collect(Collectors.toMap(InventoryEntity::getVariantId, Function.identity()));

        Instant now = Instant.now();

        for (UUID variantId : sortedVariantIds) {
            int requested = requestedQuantities.get(variantId);
            InventoryEntity inventory = inventoryMap.get(variantId);

            int activeReserved = inventoryJpaRepository.sumActiveReservedQuantity(storeId, variantId, now);
            int available = inventory.getQuantity() - activeReserved;

            if (available < requested) {
                throw new ConflictException(
                        "INSUFFICIENT_STOCK",
                        "Stock insuficiente para la variante: " + variantId +
                                ". Disponible: " + Math.max(0, available) + ", Solicitado: " + requested
                );
            }
        }

        List<InventoryReservation> createdReservations = new ArrayList<>();
        for (UUID variantId : sortedVariantIds) {
            int quantity = requestedQuantities.get(variantId);
            InventoryReservation reservation = InventoryReservation.create(
                    storeId,
                    capacityReservationId,
                    variantId,
                    quantity,
                    expiresAt
            );
            createdReservations.add(inventoryReservationRepository.save(reservation));
        }

        return createdReservations;
    }
}
