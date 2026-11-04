package com.jaldishop.backend.inventory.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Inventory {

    private final UUID variantId;
    private final int quantity;
    private final Integer lowStockThreshold;
    private final Instant updatedAt;

    public Inventory(UUID variantId, int quantity, Integer lowStockThreshold, Instant updatedAt) {
        if (variantId == null) {
            throw new IllegalArgumentException("variantId no puede ser nulo");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity no puede ser negativo");
        }
        if (lowStockThreshold != null && lowStockThreshold < 0) {
            throw new IllegalArgumentException("lowStockThreshold no puede ser negativo");
        }
        this.variantId = variantId;
        this.quantity = quantity;
        this.lowStockThreshold = lowStockThreshold;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getVariantId() {
        return variantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isLowStock() {
        return lowStockThreshold != null && quantity <= lowStockThreshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return quantity == inventory.quantity &&
                Objects.equals(variantId, inventory.variantId) &&
                Objects.equals(lowStockThreshold, inventory.lowStockThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variantId, quantity, lowStockThreshold);
    }
}
