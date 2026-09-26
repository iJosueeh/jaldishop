package com.jaldishop.backend.cart.infrastructure.persistence.mapper;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartItem;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartItemEntity;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartItemId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CartPersistenceMapperTest {

    private CartPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CartPersistenceMapper();
    }

    @Test
    @DisplayName("Should map Cart domain to CartEntity and back")
    void shouldMapDomainToEntityAndBack() {
        UUID cartId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Instant now = Instant.now();

        CartItem item = new CartItem(cartId, variantId, 3, new BigDecimal("18.00"), "PEN", now, now);
        Cart cart = new Cart(cartId, userId, storeId, List.of(item), now, now);

        CartEntity entity = mapper.toEntity(cart);

        assertNotNull(entity);
        assertEquals(cartId, entity.getId());
        assertEquals(userId, entity.getUserId());
        assertEquals(storeId, entity.getStoreId());
        assertEquals(1, entity.getItems().size());
        assertEquals(variantId, entity.getItems().get(0).getVariantId());
        assertEquals(3, entity.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("18.00"), entity.getItems().get(0).getReferencePriceAmount());

        Cart domainBack = mapper.toDomain(entity);

        assertNotNull(domainBack);
        assertEquals(cartId, domainBack.getId());
        assertEquals(userId, domainBack.getUserId());
        assertEquals(storeId, domainBack.getStoreId());
        assertEquals(1, domainBack.getItems().size());
        assertEquals(variantId, domainBack.getItems().get(0).getVariantId());
        assertEquals(3, domainBack.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("Should map CartItemEntity to domain")
    void shouldMapItemEntityToDomain() {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        Instant now = Instant.now();

        CartItemEntity itemEntity = new CartItemEntity(
                new CartItemId(cartId, variantId),
                null,
                2,
                new BigDecimal("50.00"),
                "PEN",
                now,
                now
        );

        CartItem itemDomain = mapper.toItemDomain(itemEntity);

        assertNotNull(itemDomain);
        assertEquals(cartId, itemDomain.getCartId());
        assertEquals(variantId, itemDomain.getVariantId());
        assertEquals(2, itemDomain.getQuantity());
        assertEquals(new BigDecimal("50.00"), itemDomain.getReferencePriceAmount());
        assertEquals("PEN", itemDomain.getReferencePriceCurrency());
    }
}
