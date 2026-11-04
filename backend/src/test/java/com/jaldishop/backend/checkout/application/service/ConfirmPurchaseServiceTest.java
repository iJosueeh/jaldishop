package com.jaldishop.backend.checkout.application.service;

import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.port.PaymentReader;
import com.jaldishop.backend.checkout.application.port.PaymentResume;
import com.jaldishop.backend.checkout.application.result.CheckoutItemSnapshotAssembler;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.checkout.domain.model.CheckoutPricing;

import com.jaldishop.backend.capacity.application.reservation.service.CommitCapacityReservationService;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.cart.application.command.ClearCartCommand;
import com.jaldishop.backend.cart.application.service.ClearCartService;
import com.jaldishop.backend.cart.domain.model.Cart;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;
import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.inventory.application.service.CommitInventoryReservationService;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.payment.domain.PaymentErrorCodes;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmPurchaseServiceTest {

    @Mock
    private PaymentReader paymentReader;
    @Mock
    private CommitCapacityReservationService commitCapacityReservationService;
    @Mock
    private CommitInventoryReservationService commitInventoryReservationService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private CheckoutItemSnapshotAssembler snapshotAssembler;
    @Mock
    private ClearCartService clearCartService;

    private ConfirmPurchaseService confirmPurchaseService;

    private UUID userId;
    private UUID storeId;
    private UUID variantId;
    private UUID productId;
    private UUID paymentId;
    private UUID capacityReservationId;
    private Store testStore;
    private Cart testCart;
    private PaymentResume approvedPayment;
    private CapacityReservation committedReservation;
    private CheckoutItemSnapshot testItemSnapshot;

    @BeforeEach
    void setUp() {
        confirmPurchaseService = new ConfirmPurchaseService(
                paymentReader, commitCapacityReservationService, commitInventoryReservationService,
                orderRepository, cartRepository, storeRepository, snapshotAssembler, clearCartService
        );

        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        capacityReservationId = UUID.randomUUID();

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

        testCart = Cart.create(userId, storeId);
        testCart.addItem(variantId, 2, new BigDecimal("10.00"), "PEN");

        approvedPayment = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("25.00"), "PEN"
        );

        Instant now = Instant.now();
        committedReservation = CapacityReservation.reconstitute(
                capacityReservationId, storeId, userId,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                CapacityReservationStatus.PAYMENT_PROTECTED,
                now.plusMillis(1), now.plusMillis(1), now, now
        );
        committedReservation.commit();

        testItemSnapshot = new CheckoutItemSnapshot(
                variantId, productId, "Pan Francés", "Bolsa x10 unidades", "PAN-FR-10",
                "https://example.com/pan.png", 2, new BigDecimal("10.00"), "PEN",
                new BigDecimal("20.00"), true
        );
    }

    private ConfirmPurchaseCommand pickupCommand() {
        return new ConfirmPurchaseCommand(
                paymentId, CheckoutFulfillmentType.PICKUP,
                "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null
        );
    }

    private void stubIdempotencyMiss() {
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(orderRepository.findByCapacityReservationId(capacityReservationId)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Happy path: pago aprobado confirma la compra en PICKUP y vacía el carrito")
    void shouldConfirmPurchaseSuccessfullyForPickup() {
        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(snapshotAssembler.assemble(testCart, storeId)).thenReturn(List.of(testItemSnapshot));
        when(orderRepository.nextOrderNumber()).thenReturn(1L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderConfirmation confirmation = confirmPurchaseService.execute(pickupCommand());

        assertThat(confirmation).isNotNull();
        assertThat(confirmation.orderId()).isNotNull();
        assertThat(confirmation.orderNumber()).isEqualTo("JAL-" + LocalDate.now().getYear() + "-000001");
        assertThat(confirmation.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmation.storeId()).isEqualTo(storeId);
        assertThat(confirmation.storeName()).isEqualTo("Panadería Don Pepe");
        assertThat(confirmation.customerName()).isEqualTo("Juan Pérez");
        assertThat(confirmation.totalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(confirmation.currency()).isEqualTo("PEN");
        assertThat(confirmation.confirmedAt()).isNotNull();

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getPaymentId()).isEqualTo(paymentId);
        assertThat(saved.getCapacityReservationId()).isEqualTo(capacityReservationId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getStoreId()).isEqualTo(storeId);
        assertThat(saved.getProductsSubtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(saved.getIncludedTaxAmount()).isEqualByComparingTo(new BigDecimal("3.05"));
        assertThat(saved.getItems()).hasSize(1);

        verify(commitCapacityReservationService).execute(capacityReservationId);
        verify(commitInventoryReservationService).execute(storeId, capacityReservationId);
        verify(cartRepository).save(testCart);
        verify(clearCartService, never()).execute(any());
    }

    @Test
    @DisplayName("Happy path: DELIVERY aplica el costo de envío en el total")
    void shouldConfirmPurchaseSuccessfullyForDelivery() {
        var command = new ConfirmPurchaseCommand(
                paymentId, CheckoutFulfillmentType.DELIVERY,
                "María López", "987654322", "maria@example.com",
                "Calle Los Sauces 456", "Dpto 201",
                new BigDecimal("-12.1"), new BigDecimal("-77.1")
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(snapshotAssembler.assemble(testCart, storeId)).thenReturn(List.of(testItemSnapshot));
        when(orderRepository.nextOrderNumber()).thenReturn(2L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderConfirmation confirmation = confirmPurchaseService.execute(command);

        assertThat(confirmation.totalAmount()).isEqualByComparingTo(new BigDecimal("25.00"));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getDeliveryFeeAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(saved.getDeliveryAddress()).isEqualTo("Calle Los Sauces 456");
    }

    @Test
    @DisplayName("RN-PAY-02: un pago no aprobado no confirma ninguna compra")
    void shouldRejectPaymentNotApproved() {
        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(
                new PaymentResume(paymentId, capacityReservationId, "FAILED", null, new BigDecimal("25.00"), "PEN")
        ));

        assertRuleCode(() -> confirmPurchaseService.execute(pickupCommand()), PaymentErrorCodes.RN_PAY_02);

        verify(orderRepository, never()).save(any(Order.class));
        verify(commitInventoryReservationService, never()).execute(any(), any());
    }

    @Test
    @DisplayName("Un pago inexistente lanza ResourceNotFoundException")
    void shouldThrowWhenPaymentNotFound() {
        when(paymentReader.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> confirmPurchaseService.execute(pickupCommand()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Idempotencia: devuelve el pedido existente por paymentId sin reprocesar")
    void shouldReturnExistingOrderForDuplicatePayment() {
        Order existing = Order.createConfirmed(
                new Order.CreateData(
                        userId, storeId, paymentId, capacityReservationId,
                        com.jaldishop.backend.ordering.domain.enums.DeliveryMode.PICKUP,
                        LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                        "Juan Pérez", "987654321", "juan@example.com",
                        null, null, null, null,
                        "PEN", new BigDecimal("20.00"), BigDecimal.ZERO, null,
                        BigDecimal.ZERO, new BigDecimal("3.05"), new BigDecimal("20.00"),
                        List.of(new Order.ItemData(variantId, "Pan Francés", "Bolsa x10", 2, new BigDecimal("10.00")))
                ),
                "JAL-2026-000009"
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(existing));

        OrderConfirmation confirmation = confirmPurchaseService.execute(pickupCommand());

        assertThat(confirmation.orderId()).isEqualTo(existing.getId());
        assertThat(confirmation.orderNumber()).isEqualTo("JAL-2026-000009");

        verify(commitCapacityReservationService, never()).execute(any());
        verify(orderRepository, never()).save(any(Order.class));
        verify(clearCartService, never()).execute(any());
    }

    @Test
    @DisplayName("Idempotencia: el ganador de la carrera se resuelve vía la reserva COMMITTED")
    void shouldReturnRaceWinnerByCommittedReservation() {
        Order raceWinner = Order.createConfirmed(
                new Order.CreateData(
                        userId, storeId, paymentId, capacityReservationId,
                        com.jaldishop.backend.ordering.domain.enums.DeliveryMode.PICKUP,
                        LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                        "Juan Pérez", "987654321", "juan@example.com",
                        null, null, null, null,
                        "PEN", new BigDecimal("20.00"), BigDecimal.ZERO, null,
                        BigDecimal.ZERO, new BigDecimal("3.05"), new BigDecimal("20.00"),
                        List.of(new Order.ItemData(variantId, "Pan Francés", "Bolsa x10", 2, new BigDecimal("10.00")))
                ),
                "JAL-2026-000010"
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.empty());
        when(orderRepository.findByCapacityReservationId(capacityReservationId))
                .thenReturn(Optional.empty(), Optional.of(raceWinner));
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);

        OrderConfirmation confirmation = confirmPurchaseService.execute(pickupCommand());

        assertThat(confirmation.orderId()).isEqualTo(raceWinner.getId());
        verify(commitInventoryReservationService, never()).execute(any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Una reserva ACTIVE (sin protección de pago) impide la confirmación")
    void shouldPropagateConflictWhenReservationNotProtected() {
        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId))
                .thenThrow(new ConflictException("RESERVATION_NOT_PAYMENT_PROTECTED",
                        "La reserva de capacidad no tiene protección de pago activa y no puede comprometerse."));

        assertRuleCode(() -> confirmPurchaseService.execute(pickupCommand()), "RESERVATION_NOT_PAYMENT_PROTECTED");

        verify(orderRepository, never()).save(any(Order.class));
        verify(commitInventoryReservationService, never()).execute(any(), any());
    }

    @Test
    @DisplayName("Carrito vacío: no se compromete inventario ni se crea el pedido")
    void shouldThrowConflictWhenCartEmpty() {
        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.empty());

        assertRuleCode(() -> confirmPurchaseService.execute(pickupCommand()), "CART_EMPTY");

        verify(commitInventoryReservationService, never()).execute(any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Tienda INACTIVA impide la confirmación")
    void shouldThrowConflictWhenStoreInactive() {
        Store inactiveStore = Store.reconstitute(
                storeId, UUID.randomUUID(), "Tienda Inactiva", "tienda-inactiva",
                null, null, null, null, null, null,
                true, true, null, "PEN", null,
                StoreStatus.INACTIVE, null, null
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(inactiveStore));

        assertRuleCode(() -> confirmPurchaseService.execute(pickupCommand()), "STORE_INACTIVE");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Fase 4B Snapshot: Confirma compra exitosamente desde snapshot inmutable sin consultar el catálogo")
    void shouldConfirmPurchaseSuccessfullyFromImmutableSnapshotWithoutReadingCatalog() {
        String snapshotJson = """
                [
                    {
                        "variantId": "%s",
                        "productId": "%s",
                        "productName": "Pan Francés Snapshot",
                        "presentationName": "Bolsa x10",
                        "sku": "PAN-FR-10",
                        "imageUrl": "https://example.com/pan.png",
                        "quantity": 2,
                        "unitPriceAmount": 10.00,
                        "currency": "PEN",
                        "subtotalAmount": 20.00,
                        "tracksInventory": true
                    }
                ]
                """.formatted(variantId, productId);

        PaymentResume paymentWithSnapshot = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("20.00"), "PEN",
                "PICKUP", "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), snapshotJson
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(paymentWithSnapshot));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(orderRepository.nextOrderNumber()).thenReturn(2L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderConfirmation confirmation = confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));

        assertThat(confirmation).isNotNull();
        assertThat(confirmation.orderNumber()).isEqualTo("JAL-" + LocalDate.now().getYear() + "-000002");
        assertThat(confirmation.totalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));

        // Verificamos que NUNCA se invocó al snapshotAssembler (no se recalcularon precios ni catálogo)
        verify(snapshotAssembler, never()).assemble(any(), any());
        verify(commitInventoryReservationService).execute(storeId, capacityReservationId);
    }

    @Test
    @DisplayName("Fase 4B Snapshot: Mantiene precios del snapshot aunque el catálogo haya cambiado después del checkout")
    void shouldPreserveCheckoutPricesEvenIfCatalogPricesChangedLater() {
        // En el snapshot el precio era 10.00. El catálogo hoy podría tener 50.00, pero no se consulta.
        String snapshotJson = """
                [
                    {
                        "variantId": "%s",
                        "productId": "%s",
                        "productName": "Pan Artesanal",
                        "presentationName": "Unidad",
                        "sku": "PAN-ART-01",
                        "imageUrl": null,
                        "quantity": 3,
                        "unitPriceAmount": 10.00,
                        "currency": "PEN",
                        "subtotalAmount": 30.00,
                        "tracksInventory": true
                    }
                ]
                """.formatted(variantId, productId);

        PaymentResume paymentWithSnapshot = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("30.00"), "PEN",
                "PICKUP", "Carlos Ramos", "999888777", "carlos@test.com",
                null, null, null, null,
                new BigDecimal("30.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("4.58"), snapshotJson
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(paymentWithSnapshot));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(orderRepository.nextOrderNumber()).thenReturn(3L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderConfirmation confirmation = confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));

        assertThat(confirmation.totalAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        verify(snapshotAssembler, never()).assemble(any(), any());
    }

    @Test
    @DisplayName("Fase 4B Carrito: Elimina solo cantidades comprometidas y preserva ítems agregados posteriormente al carrito")
    void shouldSelectivelyRemoveOnlyCommittedItemsAndKeepNewItemsInCart() {
        UUID newItemVariantId = UUID.randomUUID();
        // El carrito tiene el ítem comprado (2 unidades) y un ítem nuevo agregado posteriormente (1 unidad)
        testCart.addItem(newItemVariantId, 1, new BigDecimal("15.00"), "PEN");
        assertThat(testCart.getItems()).hasSize(2);

        String snapshotJson = """
                [
                    {
                        "variantId": "%s",
                        "productId": "%s",
                        "productName": "Pan Francés",
                        "presentationName": "Bolsa x10",
                        "sku": "PAN-FR-10",
                        "imageUrl": null,
                        "quantity": 2,
                        "unitPriceAmount": 10.00,
                        "currency": "PEN",
                        "subtotalAmount": 20.00,
                        "tracksInventory": true
                    }
                ]
                """.formatted(variantId, productId);

        PaymentResume paymentWithSnapshot = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("20.00"), "PEN",
                "PICKUP", "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), snapshotJson
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(paymentWithSnapshot));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(orderRepository.nextOrderNumber()).thenReturn(4L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));

        // El ítem comprado (variantId) se eliminó del carrito, pero el nuevo ítem (newItemVariantId) permanece
        assertThat(testCart.findItemByVariantId(variantId)).isEmpty();
        assertThat(testCart.findItemByVariantId(newItemVariantId)).isPresent();
        assertThat(testCart.findItemByVariantId(newItemVariantId).get().getQuantity()).isEqualTo(1);
        verify(cartRepository).save(testCart);
        verify(clearCartService, never()).execute(any());
    }

    @Test
    @DisplayName("Fase 4B Carrito: Descuenta parcialmente la cantidad si el usuario aumentó unidades en el carrito tras el checkout")
    void shouldSelectivelyDecrementQuantityWhenCartHasHigherQuantity() {
        // En el carrito ahora hay 5 unidades (el usuario agregó 3 más tras el checkout)
        testCart.updateItemQuantity(variantId, 5);

        String snapshotJson = """
                [
                    {
                        "variantId": "%s",
                        "productId": "%s",
                        "productName": "Pan Francés",
                        "presentationName": "Bolsa x10",
                        "sku": "PAN-FR-10",
                        "imageUrl": null,
                        "quantity": 2,
                        "unitPriceAmount": 10.00,
                        "currency": "PEN",
                        "subtotalAmount": 20.00,
                        "tracksInventory": true
                    }
                ]
                """.formatted(variantId, productId);

        PaymentResume paymentWithSnapshot = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("20.00"), "PEN",
                "PICKUP", "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), snapshotJson
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(paymentWithSnapshot));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(orderRepository.nextOrderNumber()).thenReturn(5L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));

        // Quedan 5 - 2 = 3 unidades en el carrito
        assertThat(testCart.findItemByVariantId(variantId)).isPresent();
        assertThat(testCart.findItemByVariantId(variantId).get().getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("H-4B-03 Carrito: Conserva la línea del carrito cuando contiene menos unidades que las compradas (modificación posterior)")
    void shouldPreserveCartLineWhenCartHasFewerUnitsThanPurchased() {
        // En el carrito ahora hay 1 unidad (el usuario redujo de 2 a 1 tras el checkout)
        testCart.updateItemQuantity(variantId, 1);

        String snapshotJson = """
                [
                    {
                        "variantId": "%s",
                        "productId": "%s",
                        "productName": "Pan Francés",
                        "presentationName": "Bolsa x10",
                        "sku": "PAN-FR-10",
                        "imageUrl": null,
                        "quantity": 2,
                        "unitPriceAmount": 10.00,
                        "currency": "PEN",
                        "subtotalAmount": 20.00,
                        "tracksInventory": true
                    }
                ]
                """.formatted(variantId, productId);

        PaymentResume paymentWithSnapshot = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("20.00"), "PEN",
                "PICKUP", "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), snapshotJson
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(paymentWithSnapshot));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(orderRepository.nextOrderNumber()).thenReturn(6L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));

        // La línea se conserva intacta con 1 unidad para no eliminar la decisión posterior del usuario
        assertThat(testCart.findItemByVariantId(variantId)).isPresent();
        assertThat(testCart.findItemByVariantId(variantId).get().getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("H-4B-03 Carrito: Si el usuario eliminó la línea del carrito tras el checkout, no la recrea")
    void shouldNotRecreateCartItemIfUserDeletedLineAfterCheckout() {
        testCart.removeItem(variantId);
        assertThat(testCart.getItems()).isEmpty();

        String snapshotJson = """
                [
                    {
                        "variantId": "%s",
                        "productId": "%s",
                        "productName": "Pan Francés",
                        "presentationName": "Bolsa x10",
                        "sku": "PAN-FR-10",
                        "imageUrl": null,
                        "quantity": 2,
                        "unitPriceAmount": 10.00,
                        "currency": "PEN",
                        "subtotalAmount": 20.00,
                        "tracksInventory": true
                    }
                ]
                """.formatted(variantId, productId);

        PaymentResume paymentWithSnapshot = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("20.00"), "PEN",
                "PICKUP", "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), snapshotJson
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(paymentWithSnapshot));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(orderRepository.nextOrderNumber()).thenReturn(7L);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));

        // El carrito sigue vacío, no se recreó la línea
        assertThat(testCart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Fase 4B Rollback: Fallo en descuento de inventario aborta la confirmación sin crear orden")
    void shouldFailAndRollbackWhenInventoryReservationFails() {
        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));
        when(cartRepository.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(testCart));
        when(snapshotAssembler.assemble(testCart, storeId)).thenReturn(List.of(testItemSnapshot));

        // El descuento de inventario falla por stock insuficiente
        org.mockito.Mockito.doThrow(new ConflictException("INSUFFICIENT_STOCK", "Stock insuficiente"))
                .when(commitInventoryReservationService).execute(storeId, capacityReservationId);

        assertThatThrownBy(() -> confirmPurchaseService.execute(pickupCommand()))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INSUFFICIENT_STOCK"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fase 4B Rollback: Reserva de capacidad expirada aborta confirmación comercial")
    void shouldFailAndRollbackWhenCapacityReservationExpires() {
        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(approvedPayment));
        stubIdempotencyMiss();

        when(commitCapacityReservationService.execute(capacityReservationId))
                .thenThrow(new ConflictException("RESERVATION_PROTECTION_EXPIRED", "Protección vencida"));

        assertThatThrownBy(() -> confirmPurchaseService.execute(pickupCommand()))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("RESERVATION_PROTECTION_EXPIRED"));

        verify(commitInventoryReservationService, never()).execute(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fase 4B Validación Snapshot: Snapshot con subtotal inconsistente es rechazado con INVALID_SNAPSHOT")
    void shouldRejectWhenItemsSnapshotIsCorruptOrInconsistent() {
        String badSnapshotJson = """
                [
                    {
                        "variantId": "%s",
                        "productId": "%s",
                        "productName": "Pan Francés",
                        "presentationName": "Bolsa x10",
                        "sku": "PAN-FR-10",
                        "imageUrl": null,
                        "quantity": 2,
                        "unitPriceAmount": 10.00,
                        "currency": "PEN",
                        "subtotalAmount": 99.00,
                        "tracksInventory": true
                    }
                ]
                """.formatted(variantId, productId); // 2 * 10.00 = 20.00 != 99.00

        PaymentResume paymentWithBadSnapshot = new PaymentResume(
                paymentId, capacityReservationId, "APPROVED", Instant.now(),
                new BigDecimal("20.00"), "PEN",
                "PICKUP", "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), badSnapshotJson
        );

        when(paymentReader.findById(paymentId)).thenReturn(Optional.of(paymentWithBadSnapshot));
        stubIdempotencyMiss();
        when(commitCapacityReservationService.execute(capacityReservationId)).thenReturn(committedReservation);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

        assertThatThrownBy(() -> confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId)))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INVALID_SNAPSHOT"));

        verify(orderRepository, never()).save(any());
    }

    private static void assertRuleCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThat(catchThrowableOfType(callable, ConflictException.class).getCode()).isEqualTo(code);
    }

}
