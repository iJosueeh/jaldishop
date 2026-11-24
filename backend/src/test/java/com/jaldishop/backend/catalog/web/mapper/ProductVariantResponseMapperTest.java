package com.jaldishop.backend.catalog.web.mapper;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.catalog.domain.model.VariantAttribute;
import com.jaldishop.backend.catalog.web.dto.ProductVariantResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductVariantResponseMapperTest {

    private final ProductVariantResponseMapper mapper = new ProductVariantResponseMapper();

    @Test
    @DisplayName("Should map variant domain entity to ProductVariantResponse DTO")
    void shouldMapVariantToResponse() {
        UUID productId = UUID.randomUUID();
        ProductVariant variant = ProductVariant.create(
                productId,
                "Porcion Individual",
                "SKU-IND-01",
                new BigDecimal("12.50"),
                "PEN",
                true,
                List.of(new VariantAttribute("Tamano", "Pequeno"))
        );

        ProductVariantResponse response = mapper.toResponse(variant);

        assertNotNull(response);
        assertEquals(variant.getId(), response.id());
        assertEquals(productId, response.productId());
        assertEquals("Porcion Individual", response.presentationName());
        assertEquals("SKU-IND-01", response.sku());
        assertEquals(new BigDecimal("12.50"), response.priceAmount());
        assertEquals("PEN", response.priceCurrency());
        assertTrue(response.tracksInventory());
        assertNull(response.availableQuantity());
        assertEquals("ACTIVE", response.status());
        assertEquals(1, response.attributes().size());
        assertEquals("Tamano", response.attributes().get(0).name());
        assertEquals("Pequeno", response.attributes().get(0).value());
    }

    @Test
    @DisplayName("Should include availableQuantity when tracksInventory is true")
    void shouldIncludeAvailableQuantityWhenTracked() {
        ProductVariant tracked = ProductVariant.create(
                UUID.randomUUID(), "Caja", "SKU-CAJA", new BigDecimal("25.00"), "PEN", true, List.of()
        );
        ProductVariant untracked = ProductVariant.create(
                UUID.randomUUID(), "Bajo pedido", "SKU-PED", new BigDecimal("30.00"), "PEN", false, List.of()
        );

        assertEquals(7, mapper.toResponse(tracked, 7).availableQuantity());
        assertNull(mapper.toResponse(untracked, 7).availableQuantity());
    }

    @Test
    @DisplayName("Should return null when variant is null")
    void shouldReturnNullWhenVariantIsNull() {
        assertNull(mapper.toResponse(null));
    }
}
