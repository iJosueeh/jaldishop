package com.jaldishop.backend.cart.web.mapper;

import com.jaldishop.backend.cart.application.CartItemView;
import com.jaldishop.backend.cart.application.CartView;
import com.jaldishop.backend.cart.web.dto.CartItemResponse;
import com.jaldishop.backend.cart.web.dto.CartResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CartResponseMapperTest {

    private final CartResponseMapper mapper = new CartResponseMapper();

    @Test
    @DisplayName("Should map CartView to CartResponse")
    void shouldMapCartViewToResponse() {
        UUID cartId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CartItemView itemView = new CartItemView(
                variantId,
                productId,
                "Alfajor",
                "Caja x6",
                "ALF-06",
                "https://img.png",
                2,
                new BigDecimal("10.00"),
                "PEN",
                new BigDecimal("20.00"),
                true,
                true
        );

        CartView cartView = new CartView(
                cartId,
                userId,
                storeId,
                List.of(itemView),
                2,
                new BigDecimal("20.00"),
                "PEN",
                Instant.now()
        );

        CartResponse response = mapper.toResponse(cartView);

        assertNotNull(response);
        assertEquals(cartId, response.id());
        assertEquals(userId, response.userId());
        assertEquals(storeId, response.storeId());
        assertEquals(2, response.totalItems());
        assertEquals(new BigDecimal("20.00"), response.totalAmount());
        assertEquals("PEN", response.currency());
        assertEquals(1, response.items().size());

        CartItemResponse itemRes = response.items().get(0);
        assertEquals(variantId, itemRes.variantId());
        assertEquals(productId, itemRes.productId());
        assertEquals("Alfajor", itemRes.productName());
        assertEquals("Caja x6", itemRes.presentationName());
        assertEquals(2, itemRes.quantity());
    }

    @Test
    @DisplayName("Should return null when view is null")
    void shouldReturnNullWhenNull() {
        assertNull(mapper.toResponse(null));
        assertNull(mapper.toItemResponse(null));
    }
}
