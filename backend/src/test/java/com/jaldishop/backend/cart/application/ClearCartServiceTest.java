package com.jaldishop.backend.cart.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearCartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private CartViewAssembler cartViewAssembler;

    @InjectMocks
    private ClearCartService clearService;

    private UUID userId;
    private UUID storeId;
    private Store mockStore;
    private CartView mockCartView;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        mockStore = Store.reconstitute(
                storeId,
                UUID.randomUUID(),
                "Mi Tienda",
                "mi-tienda",
                "Desc",
                "999999999",
                "Direccion",
                null,
                null,
                null,
                true,
                true,
                BigDecimal.ZERO,
                "PEN",
                null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        mockCartView = new CartView(
                UUID.randomUUID(),
                userId,
                storeId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should clear cart when cart exists")
    void shouldClearCart() {
        Cart cart = Cart.create(userId, storeId);
        cart.addItem(UUID.randomUUID(), 2, new BigDecimal("12.00"), "PEN");

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        when(cartViewAssembler.assemble(any(Cart.class))).thenReturn(mockCartView);

        CartView result = clearService.execute(new ClearCartCommand(userId, storeId));

        assertNotNull(result);
        assertEquals(mockCartView, result);
        verify(cartRepository).save(any(Cart.class));
        verify(cartViewAssembler).assemble(any(Cart.class));
    }

    @Test
    @DisplayName("Should return empty view when cart does not exist")
    void shouldReturnEmptyWhenCartDoesNotExist() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.empty());
        when(cartViewAssembler.empty(userId, storeId)).thenReturn(mockCartView);

        CartView result = clearService.execute(new ClearCartCommand(userId, storeId));

        assertNotNull(result);
        assertEquals(mockCartView, result);
        verify(cartViewAssembler).empty(userId, storeId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when store not found")
    void shouldThrowWhenStoreNotFound() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                clearService.execute(new ClearCartCommand(userId, storeId))
        );
    }
}
