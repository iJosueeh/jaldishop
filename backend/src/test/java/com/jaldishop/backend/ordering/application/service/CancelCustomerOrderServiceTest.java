package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderDetailResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelCustomerOrderService — Pruebas unitarias de cancelación por cliente")
class CancelCustomerOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCancellationCompensator cancellationCompensator;

    @Mock
    private GetCustomerOrderDetailService getOrderDetailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CancelCustomerOrderService service;

    private UUID customerId;
    private UUID storeId;
    private UUID paymentId;
    private UUID capacityReservationId;
    private UUID variantId;
    private String orderNumber;

    @BeforeEach
    void setUp() {
        service = new CancelCustomerOrderService(
                orderRepository,
                cancellationCompensator,
                getOrderDetailService,
                eventPublisher
        );

        customerId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        capacityReservationId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        orderNumber = "JAL-2026-000001";
    }

    @Test
    @DisplayName("Cancelación exitosa del cliente: pedido en CONFIRMED se cancela, compensa y emite evento")
    void shouldCancelOrderSuccessfullyWhenCustomerIsOwnerAndStatusIsConfirmed() {
        Order order = createSampleConfirmedOrder();
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerOrderDetailResponse dummyResponse = mock(CustomerOrderDetailResponse.class);
        when(getOrderDetailService.execute(customerId, orderNumber)).thenReturn(dummyResponse);

        CustomerOrderDetailResponse result = service.execute(customerId, orderNumber, "Cambio de planes");

        assertThat(result).isSameAs(dummyResponse);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        verify(cancellationCompensator).compensate(eq(order), eq(OrderStatus.CONFIRMED), eq("Cambio de planes"));

        ArgumentCaptor<OrderStatusChangedDomainEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedDomainEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        OrderStatusChangedDomainEvent event = captor.getValue();
        assertThat(event.previousStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(event.newStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("Rechazo de cancelación: cliente intenta cancelar pedido ajeno (AccessDeniedException)")
    void shouldRejectCancellationWhenOrderBelongsToAnotherCustomer() {
        Order order = createSampleConfirmedOrder();
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));

        UUID anotherCustomerId = UUID.randomUUID();

        assertThatThrownBy(() -> service.execute(anotherCustomerId, orderNumber, "Cancelación ajena"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No tiene permisos para cancelar un pedido ajeno");

        verify(orderRepository, never()).save(any());
        verify(cancellationCompensator, never()).compensate(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Rechazo de cancelación: pedido en IN_PREPARATION no puede ser cancelado por el cliente (RN-ORD-09)")
    void shouldRejectCancellationWhenOrderIsInPreparation() {
        Order order = createSampleConfirmedOrder();
        order.transitionTo(OrderStatus.IN_PREPARATION, null, "Iniciando cocina");

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.execute(customerId, orderNumber, "Cancelación tardía"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Solo los pedidos en estado CONFIRMED pueden ser cancelados");

        verify(orderRepository, never()).save(any());
        verify(cancellationCompensator, never()).compensate(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Idempotencia: pedido ya CANCELLED retorna detalle sin re-compensar")
    void shouldBeIdempotentWhenOrderIsAlreadyCancelled() {
        Order order = createSampleConfirmedOrder();
        order.transitionTo(OrderStatus.CANCELLED, customerId, "Primera cancelación");

        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.of(order));
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        CustomerOrderDetailResponse dummyResponse = mock(CustomerOrderDetailResponse.class);
        when(getOrderDetailService.execute(customerId, orderNumber)).thenReturn(dummyResponse);

        CustomerOrderDetailResponse result = service.execute(customerId, orderNumber, "Segunda cancelación");

        assertThat(result).isSameAs(dummyResponse);
        verify(orderRepository, never()).save(any());
        verify(cancellationCompensator, never()).compensate(any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Rechazo: Pedido inexistente arroja ResourceNotFoundException")
    void shouldThrowWhenOrderDoesNotExist() {
        when(orderRepository.findByOrderNumber(orderNumber)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(customerId, orderNumber, "Cancelación"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(cancellationCompensator, never()).compensate(any(), any(), any());
    }

    private Order createSampleConfirmedOrder() {
        return Order.createConfirmed(
                new Order.CreateData(
                        customerId,
                        storeId,
                        paymentId,
                        capacityReservationId,
                        DeliveryMode.DELIVERY,
                        LocalDate.now().plusDays(1),
                        LocalTime.of(10, 0),
                        LocalTime.of(12, 0),
                        "Cliente Ejemplo",
                        "987654321",
                        "cliente@example.com",
                        "Av. Las Palmeras 123",
                        null,
                        null,
                        null,
                        "PEN",
                        new BigDecimal("50.00"),
                        BigDecimal.ZERO,
                        null,
                        new BigDecimal("5.00"),
                        new BigDecimal("7.63"),
                        new BigDecimal("55.00"),
                        List.of(new Order.ItemData(variantId, "Torta Chocolate", "Porción", 2, new BigDecimal("25.00")))
                ),
                orderNumber
        );
    }
}
