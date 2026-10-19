package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.capacity.application.CommitCapacityReservationService;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.cart.application.ClearCartCommand;
import com.jaldishop.backend.cart.application.ClearCartService;
import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartRepository;
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.inventory.application.CommitInventoryReservationService;
import com.jaldishop.backend.ordering.domain.Order;
import com.jaldishop.backend.ordering.domain.OrderRepository;
import com.jaldishop.backend.ordering.domain.OrderStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
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
        verify(clearCartService).execute(new ClearCartCommand(userId, storeId));
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

        assertRuleCode(() -> confirmPurchaseService.execute(pickupCommand()), "RN-PAY-02");

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
                        com.jaldishop.backend.ordering.domain.DeliveryMode.PICKUP,
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
                        com.jaldishop.backend.ordering.domain.DeliveryMode.PICKUP,
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

    private static void assertRuleCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThat(catchThrowableOfType(callable, ConflictException.class).getCode()).isEqualTo(code);
    }

}