package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeOrderStatusServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ChangeOrderStatusService service;

    private UUID orderId;
    private UUID customerId;
    private UUID storeId;
    private UUID paymentId;
    private UUID capacityReservationId;
    private UUID variantId;

    @BeforeEach
    void setUp() {
        service = new ChangeOrderStatusService(orderRepository, eventPublisher);

        customerId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        capacityReservationId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Prueba 1: Tienda autorizada -> operación permitida y publica OrderStatusChangedDomainEvent")
    void shouldChangeStatusSuccessfullyWhenStoreIsAuthorized() {
        Order order = createSampleConfirmedOrder(DeliveryMode.DELIVERY);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(order.getId(), OrderStatus.IN_PREPARATION, storeId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        verify(orderRepository).findByIdForUpdate(order.getId());
        verify(orderRepository).save(order);

        ArgumentCaptor<OrderStatusChangedDomainEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        OrderStatusChangedDomainEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(order.getId());
        assertThat(event.orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(event.customerId()).isEqualTo(order.getUserId());
        assertThat(event.storeId()).isEqualTo(order.getStoreId());
        assertThat(event.previousStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(event.newStatus()).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(event.eventType()).isEqualTo("ORDER_STATUS_CHANGED");
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Prueba 2: Tienda ajena -> operación rechazada con AccessDeniedException y sin publicar evento")
    void shouldRejectWhenOrderBelongsToAnotherStore() {
        Order order = createSampleConfirmedOrder(DeliveryMode.DELIVERY);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        UUID anotherStoreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.execute(order.getId(), OrderStatus.IN_PREPARATION, anotherStoreId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permisos para modificar un pedido de otra tienda");

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Prueba 3: Contexto de tienda nulo -> operación rechazada con AccessDeniedException")
    void shouldRejectWhenExpectedStoreIdIsNull() {
        assertThatThrownBy(() -> service.execute(orderId, OrderStatus.IN_PREPARATION, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("El contexto de tienda (expectedStoreId) es obligatorio");

        verify(orderRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Prueba 4 Idempotencia: Reintento con mismo estado -> no-op sin persistencia ni evento")
    void shouldBeNoOpWhenStatusIsAlreadySame() {
        Order order = createSampleConfirmedOrder(DeliveryMode.DELIVERY);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        service.execute(order.getId(), OrderStatus.CONFIRMED, storeId);

        verify(orderRepository).findByIdForUpdate(order.getId());
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Prueba 5: Transición inválida en Order -> lanza excepción de dominio RN-ORD-07 y no emite eventos")
    void shouldThrowRNORD07WhenTransitionIsInvalid() {
        Order order = createSampleConfirmedOrder(DeliveryMode.DELIVERY);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(order.getId(), OrderStatus.COMPLETED, storeId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Transición inválida de estado");

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Pedido no encontrado -> lanza ResourceNotFoundException y no emite eventos")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(orderId, OrderStatus.IN_PREPARATION, storeId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Transiciones válidas secuenciales emiten eventos correctos")
    void shouldPublishEventsForSubsequentValidTransitions() {
        Order order = createSampleConfirmedOrder(DeliveryMode.DELIVERY);
        order.transitionTo(OrderStatus.IN_PREPARATION, null, null);
        order.transitionTo(OrderStatus.READY, null, null);

        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(order.getId(), OrderStatus.OUT_FOR_DELIVERY, storeId);

        ArgumentCaptor<OrderStatusChangedDomainEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        OrderStatusChangedDomainEvent event = captor.getValue();
        assertThat(event.previousStatus()).isEqualTo(OrderStatus.READY);
        assertThat(event.newStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
    }

    private Order createSampleConfirmedOrder(DeliveryMode deliveryMode) {
        return Order.createConfirmed(
                new Order.CreateData(
                        customerId,
                        storeId,
                        paymentId,
                        capacityReservationId,
                        deliveryMode,
                        LocalDate.now().plusDays(1),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        "Cliente Ejemplo",
                        "987654321",
                        "cliente@example.com",
                        deliveryMode == DeliveryMode.DELIVERY ? "Av. Las Palmeras 123" : null,
                        null,
                        null,
                        null,
                        "PEN",
                        new BigDecimal("50.00"),
                        BigDecimal.ZERO,
                        null,
                        deliveryMode == DeliveryMode.DELIVERY ? new BigDecimal("5.00") : BigDecimal.ZERO,
                        new BigDecimal("7.63"),
                        deliveryMode == DeliveryMode.DELIVERY ? new BigDecimal("55.00") : new BigDecimal("50.00"),
                        List.of(new Order.ItemData(variantId, "Torta de Chocolate", "Unidad", 1, new BigDecimal("50.00")))
                ),
                "JAL-2026-000042"
        );
    }

}
