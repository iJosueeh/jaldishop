package com.jaldishop.backend.cart.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartViewAssemblerTest {

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartViewAssembler assembler;

    private UUID userId;
    private UUID storeId;
    private UUID productId;
    private UUID variantId;
    private Product mockProduct;
    private ProductVariant mockVariant;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        mockProduct = Product.create(storeId, UUID.randomUUID(), "Torta de Fresa", "torta-fresa", "Desc", "https://img.com/fresa.png");
        mockVariant = new ProductVariant(
                variantId,
                mockProduct.getId(),
                "Porción 200g",
                "SKU-FRESA-1",
                new BigDecimal("15.00"),
                "PEN",
                true,
                VariantStatus.ACTIVE,
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should assemble empty CartView")
    void shouldAssembleEmpty() {
        CartView emptyView = assembler.empty(userId, storeId);

        assertNotNull(emptyView);
        assertNull(emptyView.id());
        assertEquals(userId, emptyView.userId());
        assertEquals(storeId, emptyView.storeId());
        assertEquals(0, emptyView.totalItems());
        assertEquals(BigDecimal.ZERO, emptyView.totalAmount());
        assertTrue(emptyView.items().isEmpty());
    }

    @Test
    @DisplayName("Should assemble populated Cart with variant and product details")
    void shouldAssemblePopulatedCart() {
        Cart cart = Cart.create(userId, storeId);
        cart.addItem(variantId, 3, new BigDecimal("15.00"), "PEN");

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(mockVariant));
        when(productRepository.findById(mockVariant.getProductId())).thenReturn(Optional.of(mockProduct));

        CartView result = assembler.assemble(cart);

        assertNotNull(result);
        assertEquals(cart.getId(), result.id());
        assertEquals(userId, result.userId());
        assertEquals(storeId, result.storeId());
        assertEquals(3, result.totalItems());
        assertEquals(new BigDecimal("45.00"), result.totalAmount());
        assertEquals("PEN", result.currency());
        assertEquals(1, result.items().size());

        CartItemView itemView = result.items().get(0);
        assertEquals(variantId, itemView.variantId());
        assertEquals(mockProduct.getId(), itemView.productId());
        assertEquals("Torta de Fresa", itemView.productName());
        assertEquals("Porción 200g", itemView.presentationName());
        assertEquals("SKU-FRESA-1", itemView.sku());
        assertEquals("https://img.com/fresa.png", itemView.imageUrl());
        assertEquals(3, itemView.quantity());
        assertEquals(new BigDecimal("15.00"), itemView.unitPriceAmount());
        assertEquals(new BigDecimal("45.00"), itemView.subtotalAmount());
        assertTrue(itemView.tracksInventory());
        assertTrue(itemView.available());
    }

    @Test
    @DisplayName("Should handle missing variant gracefully using reference price fallback")
    void shouldHandleMissingVariantFallback() {
        Cart cart = Cart.create(userId, storeId);
        cart.addItem(variantId, 2, new BigDecimal("20.00"), "USD");

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.empty());

        CartView result = assembler.assemble(cart);

        assertNotNull(result);
        assertEquals(2, result.totalItems());
        assertEquals(new BigDecimal("40.00"), result.totalAmount());
        assertEquals(1, result.items().size());

        CartItemView itemView = result.items().get(0);
        assertEquals("Producto", itemView.productName());
        assertEquals(new BigDecimal("20.00"), itemView.unitPriceAmount());
        assertEquals("USD", itemView.unitPriceCurrency());
        assertFalse(itemView.available());
    }
}
