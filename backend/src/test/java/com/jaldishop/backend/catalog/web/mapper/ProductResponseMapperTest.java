package com.jaldishop.backend.catalog.web.mapper;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseMapperTest {

    private final ProductResponseMapper mapper = new ProductResponseMapper();

    @Test
    @DisplayName("Should map product domain entity to ProductResponse DTO")
    void shouldMapProductToResponse() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Cheesecake", "cheesecake", "Desc", "https://img.png");

        ProductResponse response = mapper.toResponse(product);

        assertNotNull(response);
        assertEquals(product.getId(), response.id());
        assertEquals(storeId, response.storeId());
        assertEquals(categoryId, response.categoryId());
        assertEquals("Cheesecake", response.name());
        assertEquals("cheesecake", response.slug());
        assertEquals("Desc", response.description());
        assertEquals("https://img.png", response.imageUrl());
        assertEquals(java.util.List.of("https://img.png"), response.images());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    @DisplayName("Should normalize ordered images and deduplicate fallback imageUrl")
    void shouldNormalizeOrderedImages() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Torta", "torta", "Desc", "https://img-1.png");

        ProductResponse response = mapper.toResponse(
                product,
                java.util.List.of(),
                java.util.Collections.emptyMap(),
                java.util.Arrays.asList(" https://img-1.png ", "", "https://img-2.png", "https://img-1.png")
        );

        assertEquals("https://img-1.png", response.imageUrl());
        assertEquals(java.util.List.of("https://img-1.png", "https://img-2.png"), response.images());
    }

    @Test
    @DisplayName("Should map product with variants and compute minPrice and maxPrice")
    void shouldMapProductWithVariants() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Cheesecake", "cheesecake", "Desc", "https://img.png");

        com.jaldishop.backend.catalog.domain.model.ProductVariant v1 = com.jaldishop.backend.catalog.domain.model.ProductVariant.create(
                product.getId(), "Porción", "SKU-1", new java.math.BigDecimal("15.50"), "PEN", false, null
        );
        com.jaldishop.backend.catalog.domain.model.ProductVariant v2 = com.jaldishop.backend.catalog.domain.model.ProductVariant.create(
                product.getId(), "Entero", "SKU-2", new java.math.BigDecimal("45.00"), "PEN", false, null
        );

        ProductResponse response = mapper.toResponse(product, java.util.List.of(v1, v2));

        assertNotNull(response);
        assertEquals(new java.math.BigDecimal("15.50"), response.minPrice());
        assertEquals(new java.math.BigDecimal("45.00"), response.maxPrice());
        assertEquals(2, response.variants().size());
    }

    @Test
    @DisplayName("Should return null when product is null")
    void shouldReturnNullWhenProductIsNull() {
        assertNull(mapper.toResponse(null));
    }
}
