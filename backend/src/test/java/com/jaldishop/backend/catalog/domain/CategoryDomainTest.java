package com.jaldishop.backend.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryDomainTest {

    @Test
    @DisplayName("Should create active category with default timestamps")
    void shouldCreateActiveCategory() {
        UUID storeId = UUID.randomUUID();
        Category category = Category.create(storeId, "Bebidas", "Bebidas frías y calientes");

        assertNotNull(category.getId());
        assertEquals(storeId, category.getStoreId());
        assertEquals("Bebidas", category.getName());
        assertEquals("Bebidas frías y calientes", category.getDescription());
        assertEquals(CategoryStatus.ACTIVE, category.getStatus());
        assertNotNull(category.getCreatedAt());
        assertNotNull(category.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update category details and change status")
    void shouldUpdateCategoryAndStatus() {
        Category category = Category.create(UUID.randomUUID(), "Postres", "Dulces varios");

        category.update("Postres Caseros", "Tartas y pays");
        assertEquals("Postres Caseros", category.getName());
        assertEquals("Tartas y pays", category.getDescription());

        category.deactivate();
        assertEquals(CategoryStatus.INACTIVE, category.getStatus());

        category.activate();
        assertEquals(CategoryStatus.ACTIVE, category.getStatus());
    }
}