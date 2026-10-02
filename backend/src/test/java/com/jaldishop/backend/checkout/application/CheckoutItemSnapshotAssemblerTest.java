package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutItemSnapshotAssemblerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private CheckoutItemSnapshotAssembler snapshotAssembler;

    private UUID userId;
    private UUID storeId;
    private UUID productId;
    private UUID variantId;
    private Product testProduct;
    private ProductVariant testVariant;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        testProduct = new Product(
                productId,
                storeId,
                UUID.randomUUID(),
                "Pan Francés",
                "pan-frances",
                "Pan artesanal",
                "https://example.com/pan.png",
                ProductStatus.ACTIVE,
                null,
                null
        );

        testVariant = new ProductVariant(
                variantId,
                productId,
                "Bolsa x10",
                "PAN-FR-10",
                new BigDecimal("10.00"),
                "PEN",
                true,
                VariantStatus.ACTIVE,
                List.of(),
                null,
                null
        );

        testCart = Cart.create(userId, storeId);
        testCart.addItem(variantId, 2, new BigDecimal("10.00"), "PEN");
    }

    @Test
    @DisplayName("Debe ensamblar los snapshots de los ítems exitosamente")
    void shouldAssembleSnapshotsSuccessfully() {
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        List<CheckoutItemSnapshot> result = snapshotAssembler.assemble(testCart, storeId);

        assertThat(result).hasSize(1);
        CheckoutItemSnapshot item = result.get(0);
        assertThat(item.variantId()).isEqualTo(variantId);
        assertThat(item.productId()).isEqualTo(productId);
        assertThat(item.productName()).isEqualTo("Pan Francés");
        assertThat(item.presentationName()).isEqualTo("Bolsa x10");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.subtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(item.currency()).isEqualTo("PEN");
        assertThat(item.tracksInventory()).isTrue();
    }

    @Test
    @DisplayName("Debe retornar lista vacía si el carrito es nulo")
    void shouldReturnEmptyListWhenCartNull() {
        List<CheckoutItemSnapshot> result = snapshotAssembler.assemble(null, storeId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la variante no existe")
    void shouldThrowNotFoundWhenVariantMissing() {
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snapshotAssembler.assemble(testCart, storeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("no encontrada");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si la variante está INACTIVA")
    void shouldThrowConflictWhenVariantInactive() {
        ProductVariant inactiveVariant = new ProductVariant(
                variantId, productId, "Bolsa x10", "PAN-FR-10",
                new BigDecimal("10.00"), "PEN", true, VariantStatus.INACTIVE, List.of(), null, null
        );

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(inactiveVariant));

        assertThatThrownBy(() -> snapshotAssembler.assemble(testCart, storeId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no se encuentra disponible");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si el producto pertenece a otra tienda")
    void shouldThrowConflictWhenStoreMismatch() {
        UUID otherStoreId = UUID.randomUUID();
        Product mismatchedProduct = new Product(
                productId, otherStoreId, UUID.randomUUID(),
                "Pan Francés", "pan-frances", "Pan", null, ProductStatus.ACTIVE, null, null
        );

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productRepository.findById(productId)).thenReturn(Optional.of(mismatchedProduct));

        assertThatThrownBy(() -> snapshotAssembler.assemble(testCart, storeId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no pertenece a esta tienda");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si el producto está INACTIVO")
    void shouldThrowConflictWhenProductInactive() {
        Product inactiveProduct = new Product(
                productId, storeId, UUID.randomUUID(),
                "Pan Francés", "pan-frances", "Pan", null, ProductStatus.INACTIVE, null, null
        );

        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productRepository.findById(productId)).thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> snapshotAssembler.assemble(testCart, storeId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no se encuentra activo");
    }
}
