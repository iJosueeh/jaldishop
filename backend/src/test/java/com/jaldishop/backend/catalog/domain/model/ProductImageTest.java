package com.jaldishop.backend.catalog.domain.model;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductImageTest {

    @Test
    @DisplayName("Crear imagen de producto exitosamente")
    void createProductImageSuccessfully() {
        UUID productId = UUID.randomUUID();
        ProductImage image = ProductImage.create(productId, "https://example.com/torta.jpg", 0, true);

        assertNotNull(image.getId());
        assertEquals(productId, image.getProductId());
        assertEquals("https://example.com/torta.jpg", image.getImageUrl());
        assertEquals(0, image.getPosition());
        assertTrue(image.isPrimary());
        assertNotNull(image.getCreatedAt());
    }

    @Test
    @DisplayName("Lanzar excepción cuando la URL está vacía o la posición es negativa")
    void throwWhenInvalidFields() {
        UUID productId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> ProductImage.create(productId, "  ", 0, false));
        assertThrows(IllegalArgumentException.class, () -> ProductImage.create(productId, "https://example.com/img.png", -1, false));
    }
}
