package com.jaldishop.backend.inventory.domain;

import java.time.Instant;
import java.util.UUID;

public class InventoryReservation {

    private final UUID id;
    private final UUID capacityReservationId;
    private final UUID variantId;
    private final int quantity;
    private InventoryReservationStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public InventoryReservation(UUID id, UUID capacityReservationId, UUID variantId, int quantity,
                                InventoryReservationStatus status, Instant expiresAt,
                                Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la reserva de inventario no puede ser nulo.");
        }
        if (capacityReservationId == null) {
            throw new IllegalArgumentException("El ID de la reserva de capacidad no puede ser nulo.");
        }
        if (variantId == null) {
            throw new IllegalArgumentException("El ID de la variante no puede ser nulo.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad reservada debe ser mayor a 0.");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("La fecha de expiración no puede ser nula.");
        }
        this.id = id;
        this.capacityReservationId = capacityReservationId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.status = status != null ? status : InventoryReservationStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static InventoryReservation create(UUID capacityReservationId, UUID variantId, int quantity, Instant expiresAt) {
        Instant now = Instant.now();
        return new InventoryReservation(
                UUID.randomUUID(),
                capacityReservationId,
                variantId,
                quantity,
                InventoryReservationStatus.ACTIVE,
                expiresAt,
                now,
                now
        );
    }

    public void commit() {
        if (this.status != InventoryReservationStatus.ACTIVE) {
            throw new IllegalStateException("Solo se pueden comprometer reservas de inventario activas.");
        }
        this.status = InventoryReservationStatus.COMMITTED;
        this.updatedAt = Instant.now();
    }

    public void release() {
        if (this.status != InventoryReservationStatus.ACTIVE) {
            throw new IllegalStateException("Solo se pueden liberar reservas de inventario activas.");
        }
        this.status = InventoryReservationStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        if (this.status == InventoryReservationStatus.ACTIVE) {
            this.status = InventoryReservationStatus.EXPIRED;
            this.updatedAt = Instant.now();
        }
    }

    public boolean isExpired(Instant now) {
        return this.status == InventoryReservationStatus.ACTIVE && expiresAt.isBefore(now);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCapacityReservationId() {
        return capacityReservationId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public InventoryReservationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
