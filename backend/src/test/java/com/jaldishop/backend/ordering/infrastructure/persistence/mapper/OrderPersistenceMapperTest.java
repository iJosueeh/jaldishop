package com.jaldishop.backend.ordering.infrastructure.persistence.mapper;

import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.domain.Order;
import com.jaldishop.backend.ordering.domain.OrderItem;
import com.jaldishop.backend.ordering.domain.OrderStatus;
import com.jaldishop.backend.ordering.domain.OrderStatusHistory;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderItemEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPersistenceMapperTest {

    private final OrderPersistenceMapper mapper = new OrderPersistenceMapper();

    private Order order;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        order = Order.createConfirmed(new Order.CreateData(
                userId, storeId, paymentId, reservationId, DeliveryMode.PICKUP,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Juan Pérez", "987654321", "juan@example.com",
                null, null, null, null,
                "PEN", new BigDecimal("20.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("3.05"), new BigDecimal("20.00"),
                List.of(new Order.ItemData(variantId, "Pan Francés", "Bolsa x10", 2, new BigDecimal("10.00")))
        ), "JAL-2026-000001");
    }

    @Test
    @DisplayName("toEntity mapea el agregado completo incluyendo store_id en items e historial")
    void shouldMapDomainToEntity() {
        OrderEntity entity = mapper.toEntity(order);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(order.getId());
        assertThat(entity.getOrderNumber()).isEqualTo("JAL-2026-000001");
        assertThat(entity.getUserId()).isEqualTo(order.getUserId());
        assertThat(entity.getStoreId()).isEqualTo(order.getStoreId());
        assertThat(entity.getPaymentId()).isEqualTo(order.getPaymentId());
        assertThat(entity.getCapacityReservationId()).isEqualTo(order.getCapacityReservationId());
        assertThat(entity.getDeliveryMode()).isEqualTo(DeliveryMode.PICKUP);
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(entity.getCurrency()).isEqualTo("PEN");
        assertThat(entity.getTotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(entity.getConfirmedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();

        assertThat(entity.getItems()).hasSize(1);
        OrderItemEntity itemEntity = entity.getItems().getFirst();
        assertThat(itemEntity.getOrderId()).isEqualTo(order.getId());
        assertThat(itemEntity.getStoreId()).isEqualTo(order.getStoreId());
        assertThat(itemEntity.getVariantId()).isEqualTo(order.getItems().getFirst().variantId());
        assertThat(itemEntity.getQuantity()).isEqualTo(2);
        assertThat(itemEntity.getUnitPriceAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(itemEntity.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));

        assertThat(entity.getStatusHistory()).hasSize(1);
        OrderStatusHistoryEntity historyEntity = entity.getStatusHistory().getFirst();
        assertThat(historyEntity.getOrderId()).isEqualTo(order.getId());
        assertThat(historyEntity.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(historyEntity.getReason()).isEqualTo("Pedido confirmado por pago aprobado.");
    }

    @Test
    @DisplayName("toDomain reconstituye el agregado con ítems e historial")
    void shouldMapEntityToDomain() {
        Order recomposed = mapper.toDomain(mapper.toEntity(order));

        assertThat(recomposed).isNotNull();
        assertThat(recomposed.getId()).isEqualTo(order.getId());
        assertThat(recomposed.getOrderNumber()).isEqualTo("JAL-2026-000001");
        assertThat(recomposed.getUserId()).isEqualTo(order.getUserId());
        assertThat(recomposed.getStoreId()).isEqualTo(order.getStoreId());
        assertThat(recomposed.getPaymentId()).isEqualTo(order.getPaymentId());
        assertThat(recomposed.getCapacityReservationId()).isEqualTo(order.getCapacityReservationId());
        assertThat(recomposed.getDeliveryMode()).isEqualTo(DeliveryMode.PICKUP);
        assertThat(recomposed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(recomposed.getCurrency()).isEqualTo("PEN");
        assertThat(recomposed.getConfirmedAt()).isEqualTo(order.getConfirmedAt());

        assertThat(recomposed.getItems()).hasSize(1);
        OrderItem item = recomposed.getItems().getFirst();
        assertThat(item.orderId()).isEqualTo(order.getId());
        assertThat(item.variantId()).isEqualTo(order.getItems().getFirst().variantId());
        assertThat(item.subtotalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));

        assertThat(recomposed.getStatusHistory()).hasSize(1);
        OrderStatusHistory history = recomposed.getStatusHistory().getFirst();
        assertThat(history.orderId()).isEqualTo(order.getId());
        assertThat(history.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(history.reason()).isEqualTo("Pedido confirmado por pago aprobado.");
    }

    @Test
    @DisplayName("round trip conserva transiciones de estado e historial incremental")
    void shouldPreserveStatusHistoryAfterTransitionRoundTrip() {
        order.transitionTo(OrderStatus.IN_PREPARATION, UUID.randomUUID(), "Inicio de preparación");

        Order recomposed = mapper.toDomain(mapper.toEntity(order));

        assertThat(recomposed.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(recomposed.getStatusHistory()).hasSize(2);
        assertThat(recomposed.getStatusHistory().getLast().status()).isEqualTo(OrderStatus.IN_PREPARATION);
    }

    @Test
    @DisplayName("toDomain(null) y toEntity(null) retornan null")
    void shouldReturnNullForNullInput() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }

}