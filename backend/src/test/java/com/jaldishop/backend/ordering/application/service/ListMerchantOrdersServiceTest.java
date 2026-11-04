package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;

import com.jaldishop.backend.ordering.application.result.MerchantOrderItemResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderPageResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderSummaryResponse;
import com.jaldishop.backend.ordering.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListMerchantOrdersServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ListMerchantOrdersService listMerchantOrdersService;

    private UUID storeId;
    private UUID customerId;
    private UUID orderId;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        OrderItem item1 = new OrderItem(
                UUID.randomUUID(), orderId, UUID.randomUUID(), "Torta de Chocolate",
                "Porción Individual", 2, new BigDecimal("15.00"), new BigDecimal("30.00")
        );
        OrderItem item2 = new OrderItem(
                UUID.randomUUID(), orderId, UUID.randomUUID(), "Café Americano",
                "12 oz", 1, new BigDecimal("8.00"), new BigDecimal("8.00")
        );

        sampleOrder = Order.reconstitute(
                orderId, "JAL-2026-000100", customerId, storeId,
                UUID.randomUUID(), UUID.randomUUID(), DeliveryMode.DELIVERY,
                LocalDate.of(2026, 11, 1), LocalTime.of(14, 0), LocalTime.of(16, 0),
                "María López", "987654321", "maria@ejemplo.pe",
                "Av. Larco 456", "Dpto 302", new BigDecimal("-12.122"), new BigDecimal("-77.028"),
                "PEN", new BigDecimal("38.00"), BigDecimal.ZERO, null,
                new BigDecimal("5.00"), new BigDecimal("6.56"), new BigDecimal("43.00"),
                Instant.parse("2026-10-31T10:00:00Z"), OrderStatus.CONFIRMED,
                Instant.parse("2026-10-31T10:00:00Z"), List.of(item1, item2), Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si storeId es nulo")
    void shouldThrowExceptionWhenStoreIdIsNull() {
        assertThatThrownBy(() -> listMerchantOrdersService.execute(null, null, PageRequest.of(0, 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El identificador de la tienda es obligatorio.");

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Listado paginado sin filtro: consulta todos los pedidos de la tienda y mapea metadatos y modelos")
    void shouldListOrdersWithoutFilterWithPagination() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> domainPage = new PageImpl<>(List.of(sampleOrder), pageable, 1);

        when(orderRepository.findByStoreId(storeId, pageable)).thenReturn(domainPage);

        MerchantOrderPageResponse response = listMerchantOrdersService.execute(storeId, null, pageable);

        assertThat(response).isNotNull();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();

        assertThat(response.content()).hasSize(1);
        MerchantOrderSummaryResponse summary = response.content().get(0);
        assertThat(summary.id()).isEqualTo(orderId);
        assertThat(summary.orderNumber()).isEqualTo("JAL-2026-000100");
        assertThat(summary.storeId()).isEqualTo(storeId);
        assertThat(summary.customerUserId()).isEqualTo(customerId);
        assertThat(summary.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(summary.deliveryMode()).isEqualTo(DeliveryMode.DELIVERY);
        assertThat(summary.serviceDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(summary.customerName()).isEqualTo("María López");
        assertThat(summary.customerPhone()).isEqualTo("987654321");
        assertThat(summary.deliveryAddress()).isEqualTo("Av. Larco 456");
        assertThat(summary.totalAmount()).isEqualByComparingTo("43.00");
        assertThat(summary.currency()).isEqualTo("PEN");
        assertThat(summary.totalItems()).isEqualTo(3); // 2 + 1
        assertThat(summary.items()).hasSize(2);

        MerchantOrderItemResponse item0 = summary.items().get(0);
        assertThat(item0.productName()).isEqualTo("Torta de Chocolate");
        assertThat(item0.quantity()).isEqualTo(2);
        assertThat(item0.subtotalAmount()).isEqualByComparingTo("30.00");

        verify(orderRepository).findByStoreId(storeId, pageable);
        verify(orderRepository, never()).findByStoreIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Listado filtrado por estado: consulta únicamente los pedidos con el estado solicitado")
    void shouldListOrdersWithStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> domainPage = new PageImpl<>(List.of(sampleOrder), pageable, 1);

        when(orderRepository.findByStoreIdAndStatus(storeId, OrderStatus.IN_PREPARATION, pageable))
                .thenReturn(domainPage);

        MerchantOrderPageResponse response = listMerchantOrdersService.execute(storeId, OrderStatus.IN_PREPARATION, pageable);

        assertThat(response).isNotNull();
        assertThat(response.totalElements()).isEqualTo(1);
        verify(orderRepository).findByStoreIdAndStatus(storeId, OrderStatus.IN_PREPARATION, pageable);
        verify(orderRepository, never()).findByStoreId(any(), any());
    }

    @Test
    @DisplayName("Página predeterminada: si pageable es nulo, asigna por defecto página 0 con tamaño 20")
    void shouldDefaultToFirstPageWhenPageableIsNull() {
        Page<Order> domainPage = new PageImpl<>(List.of(sampleOrder), PageRequest.of(0, 20), 1);
        when(orderRepository.findByStoreId(eq(storeId), any(Pageable.class))).thenReturn(domainPage);

        MerchantOrderPageResponse response = listMerchantOrdersService.execute(storeId, null, null);

        assertThat(response).isNotNull();
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository).findByStoreId(eq(storeId), captor.capture());
        Pageable captured = captor.getValue();
        assertThat(captured.getPageNumber()).isZero();
        assertThat(captured.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Tienda sin pedidos: retorna respuesta vacía con metadatos consistentes")
    void shouldReturnEmptyPageWhenStoreHasNoOrders() {
        Pageable pageable = PageRequest.of(0, 20);
        when(orderRepository.findByStoreId(storeId, pageable)).thenReturn(Page.empty(pageable));

        MerchantOrderPageResponse response = listMerchantOrdersService.execute(storeId, null, pageable);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    @DisplayName("Página fuera de rango: retorna contenido vacío pero con total de elementos y páginas reales")
    void shouldHandleOutOfRangePageGracefully() {
        Pageable outOfRangePageable = PageRequest.of(5, 20);
        Page<Order> emptyOutOfRangePage = new PageImpl<>(Collections.emptyList(), outOfRangePageable, 10);
        when(orderRepository.findByStoreId(storeId, outOfRangePageable)).thenReturn(emptyOutOfRangePage);

        MerchantOrderPageResponse response = listMerchantOrdersService.execute(storeId, null, outOfRangePageable);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(10);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
    }
}
