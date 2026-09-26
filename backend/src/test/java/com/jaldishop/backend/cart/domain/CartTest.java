package com.jaldishop.backend.cart.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    @Test
    @DisplayName("Should create Cart successfully")
    void shouldCreateCart() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();

        Cart cart = Cart.create(userId, storeId);

        assertNotNull(cart.getId());
        assertEquals(userId, cart.getUserId());
        assertEquals(storeId, cart.getStoreId());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getTotalQuantity());
        assertEquals(BigDecimal.ZERO, cart.calculateTotalAmount());
    }

    @Test
    @DisplayName("Should add items and aggregate quantity when same variant added again")
    void shouldAddItemsAndAggregateQuantity() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID variant1 = UUID.randomUUID();
        UUID variant2 = UUID.randomUUID();

        Cart cart = Cart.create(userId, storeId);

        cart.addItem(variant1, 2, new BigDecimal("10.00"), "PEN");
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getTotalQuantity());
        assertEquals(new BigDecimal("20.00"), cart.calculateTotalAmount());

        // Add more of variant1
        cart.addItem(variant1, 3, new BigDecimal("10.00"), "PEN");
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getTotalQuantity());
        assertEquals(new BigDecimal("50.00"), cart.calculateTotalAmount());

        // Add variant2
        cart.addItem(variant2, 1, new BigDecimal("30.00"), "PEN");
        assertEquals(2, cart.getItems().size());
        assertEquals(6, cart.getTotalQuantity());
        assertEquals(new BigDecimal("80.00"), cart.calculateTotalAmount());
    }

    @Test
    @DisplayName("Should update quantity and remove item when quantity is zero or less")
    void shouldUpdateQuantityAndRemoveWhenZero() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID variant1 = UUID.randomUUID();

        Cart cart = Cart.create(userId, storeId);
        cart.addItem(variant1, 5, new BigDecimal("10.00"), "PEN");

        cart.updateItemQuantity(variant1, 2);
        assertEquals(2, cart.getTotalQuantity());
        assertEquals(new BigDecimal("20.00"), cart.calculateTotalAmount());

        // Update to 0 removes item
        cart.updateItemQuantity(variant1, 0);
        assertTrue(cart.getItems().isEmpty());
        assertEquals(0, cart.getTotalQuantity());
    }

    @Test
    @DisplayName("Should remove item and clear all items")
    void shouldRemoveAndClear() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID variant1 = UUID.randomUUID();
        UUID variant2 = UUID.randomUUID();

        Cart cart = Cart.create(userId, storeId);
        cart.addItem(variant1, 2, new BigDecimal("10.00"), "PEN");
        cart.addItem(variant2, 1, new BigDecimal("20.00"), "PEN");

        cart.removeItem(variant1);
        assertEquals(1, cart.getItems().size());
        assertEquals(variant2, cart.getItems().get(0).getVariantId());

        cart.clear();
        assertTrue(cart.getItems().isEmpty());
    }
}
