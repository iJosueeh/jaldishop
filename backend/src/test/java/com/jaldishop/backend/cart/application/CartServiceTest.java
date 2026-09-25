package com.jaldishop.backend.cart.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
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
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartViewAssembler cartViewAssembler;

    @InjectMocks
    private CartService cartService;

    private UUID userId;
    private UUID storeId;
    private UUID productId;
    private UUID variantId;
    private Store mockStore;
    private Product mockProduct;
    private ProductVariant mockVariant;
    private CartView mockCartView;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

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
                false,
                null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
        mockProduct = Product.create(storeId, UUID.randomUUID(), "Torta de Chocolate", "torta-chocolate", "Rica torta", "image.png");
        mockVariant = new ProductVariant(
                variantId,
                mockProduct.getId(),
                "Porción 250g",
                "SKU-1",
                new BigDecimal("12.00"),
                "PEN",
                true,
                VariantStatus.ACTIVE,
                List.of(),
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
    @DisplayName("Should return empty CartView when cart does not exist")
    void shouldReturnEmptyCartViewWhenCartDoesNotExist() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.empty());
        when(cartViewAssembler.empty(userId, storeId)).thenReturn(mockCartView);

        CartView result = cartService.getCart(userId, storeId);

        assertNotNull(result);
        assertEquals(mockCartView, result);
        verify(cartViewAssembler).empty(userId, storeId);
    }

    @Test
    @DisplayName("Should add item to cart successfully")
    void shouldAddItemToCartSuccessfully() {
        var command = new AddItemToCartCommand(storeId, variantId, 2);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(mockVariant));
        when(productRepository.findById(mockVariant.getProductId())).thenReturn(Optional.of(mockProduct));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        when(cartViewAssembler.assemble(any(Cart.class))).thenReturn(mockCartView);

        CartView result = cartService.addItemToCart(userId, command);

        assertNotNull(result);
        assertEquals(mockCartView, result);
        verify(cartRepository).save(any(Cart.class));
        verify(cartViewAssembler).assemble(any(Cart.class));
    }

    @Test
    @DisplayName("Should reject adding item if variant is INACTIVE")
    void shouldRejectAddingInactiveVariant() {
        mockVariant.deactivate();
        var command = new AddItemToCartCommand(storeId, variantId, 1);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(mockVariant));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                cartService.addItemToCart(userId, command)
        );

        assertEquals("VARIANT_INACTIVE", ex.getCode());
        verify(cartRepository, never()).save(any());
        verify(cartViewAssembler, never()).assemble(any());
    }

    @Test
    @DisplayName("Should reject adding item if product belongs to another store")
    void shouldRejectAddingItemFromDifferentStore() {
        UUID otherStoreId = UUID.randomUUID();
        Product otherStoreProduct = Product.create(otherStoreId, UUID.randomUUID(), "Otro Producto", "otro-prod", "Desc", null);

        var command = new AddItemToCartCommand(storeId, variantId, 1);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(mockVariant));
        when(productRepository.findById(mockVariant.getProductId())).thenReturn(Optional.of(otherStoreProduct));

        ConflictException ex = assertThrows(ConflictException.class, () ->
                cartService.addItemToCart(userId, command)
        );

        assertEquals("VARIANT_STORE_MISMATCH", ex.getCode());
        verify(cartRepository, never()).save(any());
        verify(cartViewAssembler, never()).assemble(any());
    }

    @Test
    @DisplayName("Should update cart item quantity")
    void shouldUpdateCartItemQuantity() {
        Cart cart = Cart.create(userId, storeId);
        cart.addItem(variantId, 2, new BigDecimal("12.00"), "PEN");

        var command = new UpdateCartItemQuantityCommand(storeId, variantId, 5);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        when(cartViewAssembler.assemble(any(Cart.class))).thenReturn(mockCartView);

        CartView result = cartService.updateCartItemQuantity(userId, command);

        assertNotNull(result);
        assertEquals(mockCartView, result);
        verify(cartRepository).save(any(Cart.class));
        verify(cartViewAssembler).assemble(any(Cart.class));
    }

    @Test
    @DisplayName("Should remove cart item")
    void shouldRemoveCartItem() {
        Cart cart = Cart.create(userId, storeId);
        cart.addItem(variantId, 2, new BigDecimal("12.00"), "PEN");

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        when(cartViewAssembler.assemble(any(Cart.class))).thenReturn(mockCartView);

        CartView result = cartService.removeCartItem(userId, storeId, variantId);

        assertNotNull(result);
        assertEquals(mockCartView, result);
        verify(cartRepository).save(any(Cart.class));
        verify(cartViewAssembler).assemble(any(Cart.class));
    }

    @Test
    @DisplayName("Should clear cart")
    void shouldClearCart() {
        Cart cart = Cart.create(userId, storeId);
        cart.addItem(variantId, 2, new BigDecimal("12.00"), "PEN");

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mockStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(i -> i.getArgument(0));
        when(cartViewAssembler.assemble(any(Cart.class))).thenReturn(mockCartView);

        CartView result = cartService.clearCart(userId, storeId);

        assertNotNull(result);
        assertEquals(mockCartView, result);
        verify(cartRepository).save(any(Cart.class));
        verify(cartViewAssembler).assemble(any(Cart.class));
    }
}
