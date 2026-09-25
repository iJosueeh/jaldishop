package com.jaldishop.backend.cart.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CartItemTest {

    @Test
    @DisplayName("Should create CartItem successfully with valid arguments")
    void shouldCreateCartItemSuccessfully() {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        BigDecimal price = new BigDecimal("25.50");

        CartItem item = CartItem.create(cartId, variantId, 2, price, "PEN");

        assertNotNull(item);
        assertEquals(cartId, item.getCartId());
        assertEquals(variantId, item.getVariantId());
        assertEquals(2, item.getQuantity());
        assertEquals(price, item.getReferencePriceAmount());
        assertEquals("PEN", item.getReferencePriceCurrency());
        assertEquals(new BigDecimal("51.00"), item.calculateSubtotal());
    }

    @Test
    @DisplayName("Should reject creation with zero or negative quantity")
    void shouldRejectInvalidQuantity() {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                CartItem.create(cartId, variantId, 0, new BigDecimal("10.00"), "PEN")
        );

        assertThrows(IllegalArgumentException.class, () ->
                CartItem.create(cartId, variantId, -1, new BigDecimal("10.00"), "PEN")
        );
    }

    @Test
    @DisplayName("Should reject creation with negative reference price")
    void shouldRejectNegativePrice() {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                CartItem.create(cartId, variantId, 1, new BigDecimal("-5.00"), "PEN")
        );
    }

    @Test
    @DisplayName("Should update quantity and recalculate subtotal correctly")
    void shouldUpdateQuantity() {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        CartItem item = CartItem.create(cartId, variantId, 1, new BigDecimal("15.00"), "PEN");

        item.updateQuantity(4);
        assertEquals(4, item.getQuantity());
        assertEquals(new BigDecimal("60.00"), item.calculateSubtotal());

        item.addQuantity(2);
        assertEquals(6, item.getQuantity());
        assertEquals(new BigDecimal("90.00"), item.calculateSubtotal());
    }
}
