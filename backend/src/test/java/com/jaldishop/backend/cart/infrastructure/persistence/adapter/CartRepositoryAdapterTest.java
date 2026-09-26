package com.jaldishop.backend.cart.infrastructure.persistence.adapter;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.jaldishop.backend.cart.infrastructure.persistence.mapper.CartPersistenceMapper;
import com.jaldishop.backend.cart.infrastructure.persistence.repository.CartJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartRepositoryAdapterTest {

    @Mock
    private CartJpaRepository cartJpaRepository;

    @Mock
    private CartPersistenceMapper mapper;

    @InjectMocks
    private CartRepositoryAdapter adapter;

    private UUID userId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should save cart and return domain")
    void shouldSaveCart() {
        Cart cart = Cart.create(userId, storeId);
        CartEntity entity = new CartEntity(cart.getId(), userId, storeId, cart.getCreatedAt(), cart.getUpdatedAt());

        when(mapper.toEntity(cart)).thenReturn(entity);
        when(cartJpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(cart);

        Cart result = adapter.save(cart);

        assertNotNull(result);
        assertEquals(cart.getId(), result.getId());
        verify(cartJpaRepository).save(entity);
    }

    @Test
    @DisplayName("Should find cart by userId and storeId")
    void shouldFindByUserIdAndStoreId() {
        Cart cart = Cart.create(userId, storeId);
        CartEntity entity = new CartEntity(cart.getId(), userId, storeId, cart.getCreatedAt(), cart.getUpdatedAt());

        when(cartJpaRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(cart);

        Optional<Cart> result = adapter.findByUserIdAndStoreId(userId, storeId);

        assertTrue(result.isPresent());
        assertEquals(cart.getId(), result.get().getId());
    }

    @Test
    @DisplayName("Should delete cart by ID")
    void shouldDeleteById() {
        UUID cartId = UUID.randomUUID();

        adapter.deleteById(cartId);

        verify(cartJpaRepository).deleteById(cartId);
    }
}
