package com.jaldishop.backend.inventory.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryTest {

    @Test
    @DisplayName("Crea instancia válida con lowStockThreshold configurado")
    void validInventoryCreation() {
        UUID variantId = UUID.randomUUID();
        Instant now = Instant.now();
        Inventory inventory = new Inventory(variantId, 10, 5, now);

        assertThat(inventory.getVariantId()).isEqualTo(variantId);
        assertThat(inventory.getQuantity()).isEqualTo(10);
        assertThat(inventory.getLowStockThreshold()).isEqualTo(5);
        assertThat(inventory.getUpdatedAt()).isEqualTo(now);
        assertThat(inventory.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("Calcula isLowStock true cuando quantity <= lowStockThreshold")
    void isLowStockWhenQuantityLessOrEqualThreshold() {
        UUID variantId = UUID.randomUUID();
        Inventory invEqual = new Inventory(variantId, 5, 5, null);
        assertThat(invEqual.isLowStock()).isTrue();

        Inventory invLess = new Inventory(variantId, 2, 5, null);
        assertThat(invLess.isLowStock()).isTrue();

        Inventory invZero = new Inventory(variantId, 0, 5, null);
        assertThat(invZero.isLowStock()).isTrue();
    }

    @Test
    @DisplayName("isLowStock es false cuando lowStockThreshold es null")
    void isLowStockFalseWhenThresholdNull() {
        UUID variantId = UUID.randomUUID();
        Inventory inventory = new Inventory(variantId, 0, null, null);
        assertThat(inventory.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("Rechaza variantId nulo o quantity negativa")
    void invalidArgumentsThrowException() {
        UUID variantId = UUID.randomUUID();

        assertThatThrownBy(() -> new Inventory(null, 10, 5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("variantId no puede ser nulo");

        assertThatThrownBy(() -> new Inventory(variantId, -1, 5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity no puede ser negativo");

        assertThatThrownBy(() -> new Inventory(variantId, 10, -1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowStockThreshold no puede ser negativo");
    }
}
