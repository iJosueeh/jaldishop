package com.jaldishop.backend.store.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StoreCategoryTest {

    @Test
    @DisplayName("Crear rubro de tienda exitosamente")
    void createStoreCategorySuccessfully() {
        StoreCategory category = StoreCategory.create("Pastelería y Panadería", "pasteleria-y-panaderia", "Venta de postres y panes");

        assertNotNull(category.getId());
        assertEquals("Pastelería y Panadería", category.getName());
        assertEquals("pasteleria-y-panaderia", category.getSlug());
        assertEquals("Venta de postres y panes", category.getDescription());
        assertEquals(StoreCategoryStatus.ACTIVE, category.getStatus());
        assertNotNull(category.getCreatedAt());
        assertNotNull(category.getUpdatedAt());
    }

    @Test
    @DisplayName("Actualizar rubro y cambiar estado")
    void updateAndChangeStatus() {
        StoreCategory category = StoreCategory.create("Restaurante", "restaurante", "Comidas");

        category.update("Restaurante & Bar", "Comidas y bebidas");
        assertEquals("Restaurante & Bar", category.getName());
        assertEquals("Comidas y bebidas", category.getDescription());

        category.deactivate();
        assertEquals(StoreCategoryStatus.INACTIVE, category.getStatus());

        category.activate();
        assertEquals(StoreCategoryStatus.ACTIVE, category.getStatus());
    }

    @Test
    @DisplayName("Lanzar excepción cuando nombre o slug son inválidos")
    void throwWhenInvalidFields() {
        assertThrows(IllegalArgumentException.class, () -> StoreCategory.create("", "slug", "desc"));
        assertThrows(IllegalArgumentException.class, () -> StoreCategory.create("Name", "  ", "desc"));
    }
}
