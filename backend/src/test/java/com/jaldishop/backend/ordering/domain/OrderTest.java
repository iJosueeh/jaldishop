package com.jaldishop.backend.ordering.domain;

import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class OrderTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID RESERVATION_ID = UUID.randomUUID();
    private static final UUID VARIANT_ID = UUID.randomUUID();

    private Order.CreateData validCreateData() {
        return new Order.CreateData(
                USER_ID, STORE_ID, PAYMENT_ID, RESERVATION_ID, DeliveryMode.PICKUP,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                "PEN", new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), new BigDecimal("20.00"),
                List.of(new Order.ItemData(VARIANT_ID, "Pan Francés", "Bolsa x10", 2, new BigDecimal("10.00")))
        );
    }

    @Test
    @DisplayName("createConfirmed genera un pedido CONFIRMED con ítem e historial inicial")
    void shouldCreateConfirmedOrderWithPickup() {
        Order order = Order.createConfirmed(validCreateData(), "JAL-2026-000001");

        assertThat(order.getId()).isNotNull();
        assertThat(order.getOrderNumber()).isEqualTo("JAL-2026-000001");
        assertThat(order.getUserId()).isEqualTo(USER_ID);
        assertThat(order.getStoreId()).isEqualTo(STORE_ID);
        assertThat(order.getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(order.getCapacityReservationId()).isEqualTo(RESERVATION_ID);
        assertThat(order.getDeliveryMode()).isEqualTo(DeliveryMode.PICKUP);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getCurrency()).isEqualTo("PEN");
        assertThat(order.getProductsSubtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(order.getConfirmedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();

        assertThat(order.getItems()).hasSize(1);
        OrderItem item = order.getItems().getFirst();
        assertThat(item.orderId()).isEqualTo(order.getId());
        assertThat(item.variantId()).isEqualTo(VARIANT_ID);
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.unitPriceAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(item.subtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));

        assertThat(order.getStatusHistory()).hasSize(1);
        OrderStatusHistory history = order.getStatusHistory().getFirst();
        assertThat(history.orderId()).isEqualTo(order.getId());
        assertThat(history.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(history.changedAt()).isNotNull();
    }

    @Test
    @DisplayName("RN-ORD-02: el número de orden es obligatorio")
    void shouldRejectBlankOrderNumber() {
        assertRuleCode(() -> Order.createConfirmed(validCreateData(), " "), "RN-ORD-02");
    }

    @Test
    @DisplayName("RN-ORD-03: el usuario es obligatorio")
    void shouldRejectNullUser() {
        assertRuleCode(() -> Order.createConfirmed(withUserId(validCreateData(), null), "JAL-2026-000001"), "RN-ORD-03");
    }

    @Test
    @DisplayName("RN-ORD-04: la tienda es obligatoria")
    void shouldRejectNullStore() {
        assertRuleCode(() -> Order.createConfirmed(withStoreId(validCreateData(), null), "JAL-2026-000001"), "RN-ORD-04");
    }

    @Test
    @DisplayName("RN-PAY-02: el pago es obligatorio para crear una orden")
    void shouldRejectNullPayment() {
        assertRuleCode(() -> Order.createConfirmed(withPaymentId(validCreateData(), null), "JAL-2026-000001"), "RN-PAY-02");
    }

    @Test
    @DisplayName("RN-CAP-01: la reserva de capacidad es obligatoria")
    void shouldRejectNullReservation() {
        assertRuleCode(() -> Order.createConfirmed(withReservationId(validCreateData(), null), "JAL-2026-000001"), "RN-CAP-01");
    }

    @Test
    @DisplayName("RN-DEL-01: la modalidad de entrega es obligatoria")
    void shouldRejectNullDeliveryMode() {
        assertRuleCode(() -> Order.createConfirmed(withDeliveryMode(validCreateData(), null), "JAL-2026-000001"), "RN-DEL-01");
    }

    @Test
    @DisplayName("RN-DEL-05: DELIVERY requiere dirección de entrega")
    void shouldRejectDeliveryWithoutAddress() {
        assertRuleCode(() -> Order.createConfirmed(withDeliveryMode(validCreateData(), DeliveryMode.DELIVERY), "JAL-2026-000001"), "RN-DEL-05");
    }

    @Test
    @DisplayName("RN-ORD-06: una orden debe contener al menos un ítem")
    void shouldRejectEmptyItems() {
        assertRuleCode(() -> Order.createConfirmed(withItems(validCreateData(), List.of()), "JAL-2026-000001"), "RN-ORD-06");
    }

    @Test
    @DisplayName("RN-ORD-08: la moneda es obligatoria")
    void shouldRejectBlankCurrency() {
        Order.CreateData data = new Order.CreateData(
                USER_ID, STORE_ID, PAYMENT_ID, RESERVATION_ID, DeliveryMode.PICKUP,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                " ", new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), new BigDecimal("20.00"),
                validCreateData().items()
        );
        assertRuleCode(() -> Order.createConfirmed(data, "JAL-2026-000001"), "RN-ORD-08");
    }

    @Test
    @DisplayName("RN-ORD-09: los montos no pueden ser negativos")
    void shouldRejectNegativeAmounts() {
        assertRuleCode(() -> Order.createConfirmed(
                withTotalAmount(validCreateData(), new BigDecimal("-1.00")), "JAL-2026-000001"), "RN-ORD-09");
    }

    @Test
    @DisplayName("RN-ORD-10: el descuento no puede superar el subtotal")
    void shouldRejectDiscountAboveSubtotal() {
        Order.CreateData data = new Order.CreateData(
                USER_ID, STORE_ID, PAYMENT_ID, RESERVATION_ID, DeliveryMode.PICKUP,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                "PEN", new BigDecimal("20.00"), new BigDecimal("25.00"), null,
                BigDecimal.ZERO, new BigDecimal("3.05"), new BigDecimal("20.00"),
                validCreateData().items()
        );
        assertRuleCode(() -> Order.createConfirmed(data, "JAL-2026-000001"), "RN-ORD-10");
    }

    @Test
    @DisplayName("transitionTo avanza a IN_PREPARATION y registra historial")
    void shouldTransitionAndAppendHistory() {
        Order order = Order.createConfirmed(validCreateData(), "JAL-2026-000001");

        UUID changedBy = UUID.randomUUID();
        order.transitionTo(OrderStatus.IN_PREPARATION, changedBy, "Inicio de preparación");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(order.getStatusHistory()).hasSize(2);
        OrderStatusHistory last = order.getStatusHistory().getLast();
        assertThat(last.status()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(last.changedByUserId()).isEqualTo(changedBy);
        assertThat(last.reason()).isEqualTo("Inicio de preparación");
    }

    @Test
    @DisplayName("RN-ORD-07: transición inválida CONFIRMED -> COMPLETED es rechazada")
    void shouldRejectInvalidTransition() {
        Order order = Order.createConfirmed(validCreateData(), "JAL-2026-000001");

        assertRuleCode(() -> order.transitionTo(OrderStatus.COMPLETED, null, null), "RN-ORD-07");
    }

    @Test
    @DisplayName("RN-DEL-04: OUT_FOR_DELIVERY solo aplica a modalidad DELIVERY")
    void shouldRejectOutForDeliveryForPickup() {
        Order order = Order.createConfirmed(validCreateData(), "JAL-2026-000001");
        order.transitionTo(OrderStatus.IN_PREPARATION, null, null);
        order.transitionTo(OrderStatus.READY, null, null);

        assertRuleCode(() -> order.transitionTo(OrderStatus.OUT_FOR_DELIVERY, null, null), "RN-DEL-04");
    }

    private static void assertRuleCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThat(catchThrowableOfType(callable, BusinessRuleException.class).getCode()).isEqualTo(code);
    }

    private static Order.CreateData withUserId(Order.CreateData data, UUID userId) {
        return new Order.CreateData(
                userId, data.storeId(), data.paymentId(), data.capacityReservationId(), data.deliveryMode(),
                data.serviceDate(), data.serviceStartTime(), data.serviceEndTime(),
                data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount(), data.items()
        );
    }

    private static Order.CreateData withStoreId(Order.CreateData data, UUID storeId) {
        return new Order.CreateData(
                data.userId(), storeId, data.paymentId(), data.capacityReservationId(), data.deliveryMode(),
                data.serviceDate(), data.serviceStartTime(), data.serviceEndTime(),
                data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount(), data.items()
        );
    }

    private static Order.CreateData withPaymentId(Order.CreateData data, UUID paymentId) {
        return new Order.CreateData(
                data.userId(), data.storeId(), paymentId, data.capacityReservationId(), data.deliveryMode(),
                data.serviceDate(), data.serviceStartTime(), data.serviceEndTime(),
                data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount(), data.items()
        );
    }

    private static Order.CreateData withReservationId(Order.CreateData data, UUID reservationId) {
        return new Order.CreateData(
                data.userId(), data.storeId(), data.paymentId(), reservationId, data.deliveryMode(),
                data.serviceDate(), data.serviceStartTime(), data.serviceEndTime(),
                data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount(), data.items()
        );
    }

    private static Order.CreateData withDeliveryMode(Order.CreateData data, DeliveryMode deliveryMode) {
        return new Order.CreateData(
                data.userId(), data.storeId(), data.paymentId(), data.capacityReservationId(), deliveryMode,
                data.serviceDate(), data.serviceStartTime(), data.serviceEndTime(),
                data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount(), data.items()
        );
    }

    private static Order.CreateData withItems(Order.CreateData data, List<Order.ItemData> items) {
        return new Order.CreateData(
                data.userId(), data.storeId(), data.paymentId(), data.capacityReservationId(), data.deliveryMode(),
                data.serviceDate(), data.serviceStartTime(), data.serviceEndTime(),
                data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount(), items
        );
    }

    private static Order.CreateData withTotalAmount(Order.CreateData data, BigDecimal totalAmount) {
        return new Order.CreateData(
                data.userId(), data.storeId(), data.paymentId(), data.capacityReservationId(), data.deliveryMode(),
                data.serviceDate(), data.serviceStartTime(), data.serviceEndTime(),
                data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), totalAmount, data.items()
        );
    }

}