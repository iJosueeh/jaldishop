package com.jaldishop.backend.catalog.web.mapper;

import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.VariantAttribute;
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
        assertEquals("ACTIVE", response.status());
        assertEquals(1, response.attributes().size());
        assertEquals("Tamano", response.attributes().get(0).name());
        assertEquals("Pequeno", response.attributes().get(0).value());
    }

    @Test
    @DisplayName("Should return null when variant is null")
    void shouldReturnNullWhenVariantIsNull() {
        assertNull(mapper.toResponse(null));
    }
}
