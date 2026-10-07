package com.jaldishop.backend.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryReservationTest {

    @Test
    @DisplayName("Crear reserva de stock con estado ACTIVE")
    void createInventoryReservation() {
        UUID storeId = UUID.randomUUID();
        UUID capResId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        InventoryReservation reservation = InventoryReservation.create(storeId, capResId, variantId, 3, expiresAt);

        assertNotNull(reservation.getId());
        assertEquals(storeId, reservation.getStoreId());
        assertEquals(capResId, reservation.getCapacityReservationId());
        assertEquals(variantId, reservation.getVariantId());
        assertEquals(3, reservation.getQuantity());
        assertEquals(InventoryReservationStatus.ACTIVE, reservation.getStatus());
        assertEquals(expiresAt, reservation.getExpiresAt());
        assertNotNull(reservation.getCreatedAt());
    }

    @Test
    @DisplayName("Transición de ciclo de vida: commit, release, expire")
    void lifecycleTransitions() {
        UUID storeId = UUID.randomUUID();
        UUID capResId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        InventoryReservation res1 = InventoryReservation.create(storeId, capResId, variantId, 2, expiresAt);
        res1.commit();
        assertEquals(InventoryReservationStatus.COMMITTED, res1.getStatus());

        InventoryReservation res2 = InventoryReservation.create(storeId, capResId, variantId, 2, expiresAt);
        res2.release();
        assertEquals(InventoryReservationStatus.RELEASED, res2.getStatus());

        InventoryReservation res3 = InventoryReservation.create(storeId, capResId, variantId, 2, expiresAt);
        res3.expire();
        assertEquals(InventoryReservationStatus.EXPIRED, res3.getStatus());
    }

    @Test
    @DisplayName("Lanzar excepción cuando la cantidad no es positiva o storeId nulo")
    void throwWhenInvalidQuantityOrStoreId() {
        UUID storeId = UUID.randomUUID();
        UUID capResId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        assertThrows(IllegalArgumentException.class, () -> InventoryReservation.create(null, capResId, variantId, 1, expiresAt));
        assertThrows(IllegalArgumentException.class, () -> InventoryReservation.create(storeId, capResId, variantId, 0, expiresAt));
        assertThrows(IllegalArgumentException.class, () -> InventoryReservation.create(storeId, capResId, variantId, -1, expiresAt));
    }
}
