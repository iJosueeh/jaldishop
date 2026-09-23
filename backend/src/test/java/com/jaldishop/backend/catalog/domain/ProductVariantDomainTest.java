package com.jaldishop.backend.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductVariantDomainTest {

    @Test
    @DisplayName("Should create variant with valid price and attributes")
    void shouldCreateVariantSuccessfully() {
        UUID productId = UUID.randomUUID();
        List<VariantAttribute> attributes = List.of(new VariantAttribute("Porción", "Personal"));

        ProductVariant variant = ProductVariant.create(
                productId,
                "Porción 250g",
                "SKU-CHOCO-250",
                new BigDecimal("15.50"),
                "PEN",
                true,
                attributes
        );

        assertNotNull(variant.getId());
        assertEquals(productId, variant.getProductId());
        assertEquals("Porción 250g", variant.getPresentationName());
        assertEquals("SKU-CHOCO-250", variant.getSku());
        assertEquals(new BigDecimal("15.50"), variant.getPriceAmount());
        assertEquals("PEN", variant.getPriceCurrency());
        assertTrue(variant.isTracksInventory());
        assertEquals(VariantStatus.ACTIVE, variant.getStatus());
        assertEquals(1, variant.getAttributes().size());
    }

    @Test
    @DisplayName("Should reject variant creation if price is zero or negative")
    void shouldRejectInvalidPrice() {
        UUID productId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                ProductVariant.create(productId, "Inválido", "SKU-1", BigDecimal.ZERO, "PEN", false, List.of())
        );

        assertThrows(IllegalArgumentException.class, () ->
                ProductVariant.create(productId, "Inválido", "SKU-2", new BigDecimal("-5.00"), "PEN", false, List.of())
        );
    }
}