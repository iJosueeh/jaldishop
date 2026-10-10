package com.jaldishop.backend.inventory.application;

import com.jaldishop.backend.inventory.domain.InventoryReservation;
import com.jaldishop.backend.inventory.domain.InventoryReservationRepository;
import com.jaldishop.backend.inventory.domain.InventoryReservationStatus;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingInt;

/**
 * Compromete los holds de inventario de una reserva de capacidad:
 * descuenta el stock de forma atómica condicional y marca las
 * reservas de inventario como COMMITTED.
 *
 * <p>Orden de locks obligatorio (protocolo §16): la reserva de capacidad
 * DEBE estar bloqueada (PESSIMISTIC_WRITE) ANTES de invocar este servicio;
 * internamente se adquieren los inventarios ordenados canónicamente por
 * variant_id ASC. Nunca invertir.</p>
 */
@Service
@Transactional
public class CommitInventoryReservationService {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryReservationRepository inventoryReservationRepository;

    public CommitInventoryReservationService(
            InventoryJpaRepository inventoryJpaRepository,
            InventoryReservationRepository inventoryReservationRepository
    ) {
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
    }

    public void execute(UUID storeId, UUID capacityReservationId) {
        List<InventoryReservation> reservations = inventoryReservationRepository
                .findByCapacityReservationId(capacityReservationId);

        List<InventoryReservation> activeReservations = reservations.stream()
                .filter(r -> r.getStatus() == InventoryReservationStatus.ACTIVE)
                .toList();

        if (activeReservations.isEmpty()) {
            return;
        }

        var quantitiesByVariant = activeReservations.stream()
                .collect(groupingBy(
                        InventoryReservation::getVariantId,
                        summingInt(InventoryReservation::getQuantity)
                ));

        List<UUID> sortedVariantIds = quantitiesByVariant.keySet().stream()
                .sorted()
                .toList();

        // Lock pesimista en el mismo orden canónico que el hold (variant_id ASC).
        inventoryJpaRepository.findByVariantIdInForUpdate(sortedVariantIds);

        Instant now = Instant.now();

        // VALIDATE ALL → decremento atómico de TODAS las variantes (o rollback total).
        for (UUID variantId : sortedVariantIds) {
            int quantity = quantitiesByVariant.get(variantId);
            int otherActiveReserved = inventoryJpaRepository.sumActiveReservedQuantityExcluding(
                    storeId, variantId, capacityReservationId, now);

            int affected = inventoryJpaRepository.decrementStockConditionally(
                    variantId, quantity, otherActiveReserved, now);

            if (affected == 0) {
                throw new ConflictException(
                        "INSUFFICIENT_STOCK",
                        "Stock insuficiente para confirmar la compra de la variante: " + variantId
                );
            }
        }

        // ACT ALL: comprometer los holds una vez verificado el stock de todos.
        for (InventoryReservation reservation : activeReservations) {
            reservation.commit();
            inventoryReservationRepository.save(reservation);
        }
    }

}