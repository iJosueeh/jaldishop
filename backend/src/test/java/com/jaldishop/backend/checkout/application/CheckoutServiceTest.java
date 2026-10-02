package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.capacity.application.CreateCapacityReservationCommand;
import com.jaldishop.backend.capacity.application.CreateCapacityReservationService;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CreateCapacityReservationService createCapacityReservationService;

    @InjectMocks
    private CheckoutService checkoutService;

    private UUID userId;
    private UUID storeId;
    private UUID productId;
    private UUID variantId;
    private Store testStore;
    private Product testProduct;
    private ProductVariant testVariant;
    private Cart testCart;
    private CapacityReservation testReservation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();

        testStore = Store.reconstitute(
                storeId,
                UUID.randomUUID(),
                "Panadería Don Pepe",
                "panaderia-don-pepe",
                "La mejor panadería de la ciudad",
                "987654321",
                "Av. Principal 123",
                "Frente al parque",
                new BigDecimal("-12.046374"),
                new BigDecimal("-77.042793"),
                true,
                true,
                new BigDecimal("5.00"),
                "PEN",
                new BigDecimal("18.00"),
                StoreStatus.ACTIVE,
                null,
                null
        );

        testProduct = new Product(
                productId,
                storeId,
                UUID.randomUUID(),
                "Pan Francés",
                "pan-frances",
                "Pan crocante artesanal",
                "https://example.com/pan.png",
                ProductStatus.ACTIVE,
                null,
                null
        );

        testVariant = new ProductVariant(
                variantId,
                productId,
                "Bolsa x10 unidades",
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

        testReservation = CapacityReservation.create(
                storeId,
                userId,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0)
        );
    }

    @Test
    @DisplayName("Debe iniciar el checkout exitosamente para modalidad PICKUP")
    void shouldInitiateCheckoutSuccessfullyForPickup() {
        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Juan Pérez",
                "987654321",
                "juan@example.com",
                null,
                null,
                null,
                null,
                null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(createCapacityReservationService.execute(any(CreateCapacityReservationCommand.class))).thenReturn(testReservation);

        CheckoutResult result = checkoutService.initiateCheckout(command);

        assertThat(result).isNotNull();
        assertThat(result.reservationId()).isEqualTo(testReservation.getId());
        assertThat(result.storeId()).isEqualTo(storeId);
        assertThat(result.storeName()).isEqualTo("Panadería Don Pepe");
        assertThat(result.fulfillmentType()).isEqualTo(CheckoutFulfillmentType.PICKUP);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productName()).isEqualTo("Pan Francés");
        assertThat(result.items().get(0).presentationName()).isEqualTo("Bolsa x10 unidades");
        assertThat(result.items().get(0).quantity()).isEqualTo(2);
        assertThat(result.items().get(0).subtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));

        // Pricing assertions
        assertThat(result.pricing().productsSubtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.pricing().deliveryFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.pricing().totalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(result.pricing().includedTaxAmount()).isEqualByComparingTo(new BigDecimal("3.05"));

        verify(createCapacityReservationService).execute(any(CreateCapacityReservationCommand.class));
    }

    @Test
    @DisplayName("Debe iniciar el checkout exitosamente para modalidad DELIVERY con costo de envío")
    void shouldInitiateCheckoutSuccessfullyForDeliveryWithDeliveryFee() {
        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.DELIVERY,
                LocalDate.now().plusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                "María López",
                "987654322",
                "maria@example.com",
                "Calle Los Sauces 456",
                "Dpto 201",
                null,
                null,
                null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(createCapacityReservationService.execute(any(CreateCapacityReservationCommand.class))).thenReturn(testReservation);

        CheckoutResult result = checkoutService.initiateCheckout(command);

        assertThat(result).isNotNull();
        assertThat(result.fulfillmentType()).isEqualTo(CheckoutFulfillmentType.DELIVERY);
        assertThat(result.deliveryAddress()).isEqualTo("Calle Los Sauces 456");
        assertThat(result.pricing().deliveryFeeAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.pricing().totalAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Debe lanzar ConflictException cuando el carrito no existe o está vacío")
    void shouldThrowConflictWhenCartIsEmpty() {
        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, null, null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si la tienda está INACTIVA")
    void shouldThrowConflictWhenStoreIsInactive() {
        Store inactiveStore = Store.reconstitute(
                storeId,
                UUID.randomUUID(),
                "Tienda Inactiva",
                "tienda-inactiva",
                null, null, null, null, null, null,
                true, true, null, "PEN", null,
                StoreStatus.INACTIVE, null, null
        );

        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, null, null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(inactiveStore));

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("disponible");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si delivery está deshabilitado en la tienda")
    void shouldThrowConflictWhenDeliveryNotAvailable() {
        Store noDeliveryStore = Store.reconstitute(
                storeId,
                UUID.randomUUID(),
                "Solo Local",
                "solo-local",
                null, null, null, null, null, null,
                true, false, null, "PEN", null,
                StoreStatus.ACTIVE, null, null
        );

        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.DELIVERY,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, "Av Principal 123", null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(noDeliveryStore));

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("envíos a domicilio");
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleException si DELIVERY no incluye dirección de entrega")
    void shouldThrowBusinessRuleWhenDeliveryAddressMissing() {
        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.DELIVERY,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, "", null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("dirección de entrega es obligatoria");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si PICKUP está deshabilitado en la tienda")
    void shouldThrowConflictWhenPickupNotAvailable() {
        Store noPickupStore = Store.reconstitute(
                storeId,
                UUID.randomUUID(),
                "Solo Envíos",
                "solo-envios",
                null, null, null, null, null, null,
                false, true, new BigDecimal("6.00"), "PEN", null,
                StoreStatus.ACTIVE, null, null
        );

        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, null, null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(noPickupStore));

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("retiro en tienda");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si una variante está INACTIVA")
    void shouldThrowConflictWhenVariantInactive() {
        ProductVariant inactiveVariant = new ProductVariant(
                variantId,
                productId,
                "Bolsa x10 unidades",
                "PAN-FR-10",
                new BigDecimal("10.00"),
                "PEN",
                true,
                VariantStatus.INACTIVE,
                List.of(),
                null,
                null
        );

        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, null, null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(inactiveVariant));

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no se encuentra disponible");
    }

    @Test
    @DisplayName("Debe lanzar ConflictException si el producto está INACTIVO")
    void shouldThrowConflictWhenProductInactive() {
        Product inactiveProduct = new Product(
                productId,
                storeId,
                UUID.randomUUID(),
                "Pan Francés",
                "pan-frances",
                "Pan",
                null,
                ProductStatus.INACTIVE,
                null,
                null
        );

        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, null, null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productRepository.findById(productId)).thenReturn(Optional.of(inactiveProduct));

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("no se encuentra activo");
    }

    @Test
    @DisplayName("Debe propagar ConflictException cuando la capacidad operativa está agotada")
    void shouldPropagateConflictWhenCapacityExhausted() {
        var command = new InitiateCheckoutCommand(
                userId,
                storeId,
                CheckoutFulfillmentType.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                null, null, null, null, null, null, null, null
        );

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(productVariantRepository.findById(variantId)).thenReturn(Optional.of(testVariant));
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(createCapacityReservationService.execute(any(CreateCapacityReservationCommand.class)))
                .thenThrow(new ConflictException("CAPACITY_EXHAUSTED", "No quedan cupos disponibles para la franja horaria seleccionada."));

        assertThatThrownBy(() -> checkoutService.initiateCheckout(command))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No quedan cupos disponibles");
    }
}
