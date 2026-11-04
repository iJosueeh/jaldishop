package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;

import com.jaldishop.backend.ordering.application.result.MerchantOrderDetailResponse;
import com.jaldishop.backend.ordering.domain.model.*;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMerchantOrderDetailServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private GetMerchantOrderDetailService getMerchantOrderDetailService;

    private UUID storeId;
    private UUID orderId;
    private UUID customerId;
    private UUID paymentId;
    private UUID capacityReservationId;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        capacityReservationId = UUID.randomUUID();

        OrderItem item = new OrderItem(
                UUID.randomUUID(), orderId, UUID.randomUUID(), "Pastel Selva Negra",
                "1 kg", 1, new BigDecimal("65.00"), new BigDecimal("65.00")
        );

        OrderStatusHistory history1 = new OrderStatusHistory(
                UUID.randomUUID(), orderId, null, OrderStatus.CONFIRMED,
                "Pago aprobado", Instant.parse("2026-10-31T10:00:00Z")
        );
        OrderStatusHistory history2 = new OrderStatusHistory(
                UUID.randomUUID(), orderId, customerId, OrderStatus.IN_PREPARATION,
                "Iniciado en cocina", Instant.parse("2026-10-31T10:15:00Z")
        );

        sampleOrder = Order.reconstitute(
                orderId, "JAL-2026-000555", customerId, storeId,
                paymentId, capacityReservationId, DeliveryMode.PICKUP,
                LocalDate.of(2026, 11, 2), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Juan Pérez", "912345678", "juan.perez@ejemplo.pe",
                null, null, null, null,
                "PEN", new BigDecimal("65.00"), new BigDecimal("5.00"), "PROMO5",
                BigDecimal.ZERO, new BigDecimal("9.15"), new BigDecimal("60.00"),
                Instant.parse("2026-10-31T10:00:00Z"), OrderStatus.IN_PREPARATION,
                Instant.parse("2026-10-31T10:15:00Z"), List.of(item), List.of(history1, history2)
        );
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si orderId es nulo")
    void shouldThrowExceptionWhenOrderIdIsNull() {
        assertThatThrownBy(() -> getMerchantOrderDetailService.execute(null, storeId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El identificador del pedido es obligatorio.");

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si storeId es nulo")
    void shouldThrowExceptionWhenStoreIdIsNull() {
        assertThatThrownBy(() -> getMerchantOrderDetailService.execute(orderId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El identificador de la tienda es obligatorio.");

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Detalle existente: mapea exhaustivamente todos los atributos, importes, cliente, ítems e historial")
    void shouldReturnOrderDetailWhenExistsInStore() {
        when(orderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.of(sampleOrder));

        MerchantOrderDetailResponse detail = getMerchantOrderDetailService.execute(orderId, storeId);

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(orderId);
        assertThat(detail.orderNumber()).isEqualTo("JAL-2026-000555");
        assertThat(detail.storeId()).isEqualTo(storeId);
        assertThat(detail.customerUserId()).isEqualTo(customerId);
        assertThat(detail.paymentId()).isEqualTo(paymentId);
        assertThat(detail.capacityReservationId()).isEqualTo(capacityReservationId);
        assertThat(detail.status()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(detail.deliveryMode()).isEqualTo(DeliveryMode.PICKUP);
        assertThat(detail.serviceDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(detail.serviceStartTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(detail.serviceEndTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(detail.customerName()).isEqualTo("Juan Pérez");
        assertThat(detail.customerPhone()).isEqualTo("912345678");
        assertThat(detail.customerEmail()).isEqualTo("juan.perez@ejemplo.pe");
        assertThat(detail.deliveryAddress()).isNull();
        assertThat(detail.currency()).isEqualTo("PEN");
        assertThat(detail.productsSubtotalAmount()).isEqualByComparingTo("65.00");
        assertThat(detail.discountAmount()).isEqualByComparingTo("5.00");
        assertThat(detail.discountCode()).isEqualTo("PROMO5");
        assertThat(detail.deliveryFeeAmount()).isEqualByComparingTo("0.00");
        assertThat(detail.includedTaxAmount()).isEqualByComparingTo("9.15");
        assertThat(detail.totalAmount()).isEqualByComparingTo("60.00");
        assertThat(detail.confirmedAt()).isEqualTo(Instant.parse("2026-10-31T10:00:00Z"));
        assertThat(detail.updatedAt()).isEqualTo(Instant.parse("2026-10-31T10:15:00Z"));

        // Ítems
        assertThat(detail.items()).hasSize(1);
        var itemDto = detail.items().get(0);
        assertThat(itemDto.productName()).isEqualTo("Pastel Selva Negra");
        assertThat(itemDto.variantName()).isEqualTo("1 kg");
        assertThat(itemDto.quantity()).isEqualTo(1);
        assertThat(itemDto.unitPriceAmount()).isEqualByComparingTo("65.00");
        assertThat(itemDto.subtotalAmount()).isEqualByComparingTo("65.00");

        // Historial
        assertThat(detail.statusHistory()).hasSize(2);
        var h1 = detail.statusHistory().get(0);
        assertThat(h1.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(h1.reason()).isEqualTo("Pago aprobado");
        var h2 = detail.statusHistory().get(1);
        assertThat(h2.status()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(h2.reason()).isEqualTo("Iniciado en cocina");

        verify(orderRepository).findByIdAndStoreId(orderId, storeId);
    }

    @Test
    @DisplayName("Pedido no encontrado: lanza ResourceNotFoundException sin revelar existencia")
    void shouldThrowResourceNotFoundExceptionWhenOrderDoesNotExist() {
        when(orderRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMerchantOrderDetailService.execute(orderId, storeId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Pedido")
                .hasMessageContaining(orderId.toString());

        verify(orderRepository).findByIdAndStoreId(orderId, storeId);
    }

    @Test
    @DisplayName("Aislamiento multitienda: si el pedido pertenece a otra tienda, retorna ResourceNotFoundException")
    void shouldThrowResourceNotFoundWhenOrderBelongsToAnotherStore() {
        UUID alienStoreId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(orderId, alienStoreId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getMerchantOrderDetailService.execute(orderId, alienStoreId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository).findByIdAndStoreId(orderId, alienStoreId);
    }
}
