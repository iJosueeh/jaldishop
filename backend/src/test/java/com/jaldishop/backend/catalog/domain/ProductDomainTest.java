package com.jaldishop.backend.catalog.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductDomainTest {

    @Test
    @DisplayName("Should create product with active status by default")
    void shouldCreateActiveProduct() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Product product = Product.create(
                storeId,
                categoryId,
                "Torta de Chocolate",
                "torta-de-chocolate",
                "Torta artesanal",
                "https://image.url/torta.png"
        );

        assertNotNull(product.getId());
        assertEquals(storeId, product.getStoreId());
        assertEquals(categoryId, product.getCategoryId());
        assertEquals("Torta de Chocolate", product.getName());
        assertEquals("torta-de-chocolate", product.getSlug());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }

    @Test
    @DisplayName("Should update product fields and change state")
    void shouldUpdateProductAndState() {
        Product product = Product.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Galletas",
                "galletas",
                "De avena",
                null
        );

        UUID newCategoryId = UUID.randomUUID();
        product.update(newCategoryId, "Galletas Choco", "galletas-choco", "Con chispas", "url");

        assertEquals(newCategoryId, product.getCategoryId());
        assertEquals("Galletas Choco", product.getName());
        assertEquals("galletas-choco", product.getSlug());

        product.deactivate();
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }
}