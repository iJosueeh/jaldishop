package com.jaldishop.backend.ordering.infrastructure.persistence.adapter;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.model.OrderStatusCounts;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.jaldishop.backend.ordering.infrastructure.persistence.projection.OrderStatusAggregationProjection;
import com.jaldishop.backend.ordering.infrastructure.persistence.repository.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaOrderRepositoryAdapterTest {

    @Mock
    private OrderJpaRepository jpaRepository;

    @Mock
    private OrderPersistenceMapper mapper;

    @InjectMocks
    private JpaOrderRepositoryAdapter adapter;

    private UUID storeId;
    private UUID orderId;
    private OrderEntity orderEntity;
    private Order domainOrder;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        orderEntity = new OrderEntity(
                orderId, "JAL-2026-000001", UUID.randomUUID(), storeId, UUID.randomUUID(),
                UUID.randomUUID(), OrderStatus.CONFIRMED, DeliveryMode.PICKUP,
                LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Cliente Test", "987654321", "test@jaldi.pe", null, null,
                null, null, "PEN", new BigDecimal("50.00"), BigDecimal.ZERO,
                null, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("50.00"),
                Instant.now(), Instant.now()
        );

        domainOrder = Order.reconstitute(
                orderId, "JAL-2026-000001", UUID.randomUUID(), storeId, UUID.randomUUID(),
                UUID.randomUUID(), DeliveryMode.PICKUP, LocalDate.now(),
                LocalTime.of(10, 0), LocalTime.of(12, 0), "Cliente Test",
                "987654321", "test@jaldi.pe", null, null, null, null, "PEN",
                new BigDecimal("50.00"), BigDecimal.ZERO, null, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("50.00"), Instant.now(),
                OrderStatus.CONFIRMED, Instant.now(), Collections.emptyList(), Collections.emptyList()
        );
    }

    @Test
    @DisplayName("findByStoreId: delega en jpaRepository con paginación y mapea a dominio")
    void shouldFindByStoreIdWithPagination() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderEntity> entityPage = new PageImpl<>(List.of(orderEntity), pageable, 1);

        when(jpaRepository.findByStoreIdOrderByConfirmedAtDescIdDesc(storeId, pageable)).thenReturn(entityPage);
        when(mapper.toDomain(orderEntity)).thenReturn(domainOrder);

        Page<Order> result = adapter.findByStoreId(storeId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(domainOrder);
        verify(jpaRepository).findByStoreIdOrderByConfirmedAtDescIdDesc(storeId, pageable);
        verify(mapper).toDomain(orderEntity);
    }

    @Test
    @DisplayName("findByStoreIdAndStatus: delega en jpaRepository con filtro de estado y paginación")
    void shouldFindByStoreIdAndStatusWithPagination() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<OrderEntity> entityPage = new PageImpl<>(List.of(orderEntity), pageable, 1);

        when(jpaRepository.findByStoreIdAndStatusOrderByConfirmedAtDescIdDesc(storeId, OrderStatus.CONFIRMED, pageable))
                .thenReturn(entityPage);
        when(mapper.toDomain(orderEntity)).thenReturn(domainOrder);

        Page<Order> result = adapter.findByStoreIdAndStatus(storeId, OrderStatus.CONFIRMED, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(domainOrder);
        verify(jpaRepository).findByStoreIdAndStatusOrderByConfirmedAtDescIdDesc(storeId, OrderStatus.CONFIRMED, pageable);
        verify(mapper).toDomain(orderEntity);
    }

    @Test
    @DisplayName("findByIdAndStoreId: retorna orden mapeada si existe en la tienda")
    void shouldFindByIdAndStoreIdWhenExists() {
        when(jpaRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.of(orderEntity));
        when(mapper.toDomain(orderEntity)).thenReturn(domainOrder);

        Optional<Order> result = adapter.findByIdAndStoreId(orderId, storeId);

        assertThat(result).isPresent().contains(domainOrder);
        verify(jpaRepository).findByIdAndStoreId(orderId, storeId);
    }

    @Test
    @DisplayName("findByIdAndStoreId: retorna Optional.empty si no existe para la tienda")
    void shouldReturnEmptyWhenOrderNotFoundForStore() {
        when(jpaRepository.findByIdAndStoreId(orderId, storeId)).thenReturn(Optional.empty());

        Optional<Order> result = adapter.findByIdAndStoreId(orderId, storeId);

        assertThat(result).isEmpty();
        verify(jpaRepository).findByIdAndStoreId(orderId, storeId);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("findByOrderNumberAndStoreId: retorna orden mapeada si existe en la tienda")
    void shouldFindByOrderNumberAndStoreIdWhenExists() {
        String orderNumber = "JAL-2026-000001";
        when(jpaRepository.findByOrderNumberAndStoreId(orderNumber, storeId)).thenReturn(Optional.of(orderEntity));
        when(mapper.toDomain(orderEntity)).thenReturn(domainOrder);

        Optional<Order> result = adapter.findByOrderNumberAndStoreId(orderNumber, storeId);

        assertThat(result).isPresent().contains(domainOrder);
        verify(jpaRepository).findByOrderNumberAndStoreId(orderNumber, storeId);
    }

    @Test
    @DisplayName("findByOrderNumberAndStoreId: retorna Optional.empty si el pedido no coincide con la tienda")
    void shouldReturnEmptyWhenOrderNumberNotFoundForStore() {
        String orderNumber = "JAL-2026-999999";
        when(jpaRepository.findByOrderNumberAndStoreId(orderNumber, storeId)).thenReturn(Optional.empty());

        Optional<Order> result = adapter.findByOrderNumberAndStoreId(orderNumber, storeId);

        assertThat(result).isEmpty();
        verify(jpaRepository).findByOrderNumberAndStoreId(orderNumber, storeId);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("save: delega en mapper y jpaRepository")
    void shouldSaveOrder() {
        when(mapper.toEntity(domainOrder)).thenReturn(orderEntity);
        when(jpaRepository.save(orderEntity)).thenReturn(orderEntity);
        when(mapper.toDomain(orderEntity)).thenReturn(domainOrder);

        Order saved = adapter.save(domainOrder);

        assertThat(saved).isEqualTo(domainOrder);
        verify(jpaRepository).save(orderEntity);
    }

    @Test
    @DisplayName("findByIdForUpdate: delega en jpaRepository con lock pesimista")
    void shouldFindByIdForUpdate() {
        when(jpaRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(orderEntity));
        when(mapper.toDomain(orderEntity)).thenReturn(domainOrder);

        Optional<Order> result = adapter.findByIdForUpdate(orderId);

        assertThat(result).isPresent().contains(domainOrder);
        verify(jpaRepository).findByIdForUpdate(orderId);
    }

    @Test
    @DisplayName("countByStoreIdAndServiceDate: delega en countOrdersByStoreId cuando fecha es nula")
    void shouldDelegateToCountOrdersByStoreIdWhenDateIsNull() {
        OrderStatusAggregationProjection mockProjection = mock(OrderStatusAggregationProjection.class);
        when(mockProjection.getConfirmed()).thenReturn(12L);
        when(mockProjection.getInPreparation()).thenReturn(6L);
        when(mockProjection.getReady()).thenReturn(4L);
        when(mockProjection.getOutForDelivery()).thenReturn(2L);
        when(mockProjection.getCompleted()).thenReturn(20L);
        when(mockProjection.getCancelled()).thenReturn(4L);

        when(jpaRepository.countOrdersByStoreId(storeId)).thenReturn(mockProjection);

        OrderStatusCounts counts = adapter.countByStoreIdAndServiceDate(storeId, null);

        assertThat(counts.all()).isEqualTo(48);
        assertThat(counts.confirmed()).isEqualTo(12);
        assertThat(counts.inPreparation()).isEqualTo(6);
        assertThat(counts.ready()).isEqualTo(4);
        assertThat(counts.outForDelivery()).isEqualTo(2);
        assertThat(counts.completed()).isEqualTo(20);
        assertThat(counts.cancelled()).isEqualTo(4);

        verify(jpaRepository).countOrdersByStoreId(storeId);
        verify(jpaRepository, never()).countOrdersByStoreIdAndServiceDate(any(), any());
    }

    @Test
    @DisplayName("countByStoreIdAndServiceDate: delega en countOrdersByStoreIdAndServiceDate cuando fecha es provista")
    void shouldDelegateToCountOrdersByStoreIdAndServiceDateWhenDateIsProvided() {
        LocalDate serviceDate = LocalDate.of(2026, 11, 14);
        OrderStatusAggregationProjection mockProjection = mock(OrderStatusAggregationProjection.class);
        when(mockProjection.getConfirmed()).thenReturn(4L);
        when(mockProjection.getInPreparation()).thenReturn(3L);
        when(mockProjection.getReady()).thenReturn(2L);
        when(mockProjection.getOutForDelivery()).thenReturn(0L);
        when(mockProjection.getCompleted()).thenReturn(1L);
        when(mockProjection.getCancelled()).thenReturn(0L);

        when(jpaRepository.countOrdersByStoreIdAndServiceDate(storeId, serviceDate)).thenReturn(mockProjection);

        OrderStatusCounts counts = adapter.countByStoreIdAndServiceDate(storeId, serviceDate);

        assertThat(counts.all()).isEqualTo(10);
        assertThat(counts.confirmed()).isEqualTo(4);
        assertThat(counts.inPreparation()).isEqualTo(3);
        assertThat(counts.ready()).isEqualTo(2);
        assertThat(counts.outForDelivery()).isEqualTo(0);
        assertThat(counts.completed()).isEqualTo(1);
        assertThat(counts.cancelled()).isEqualTo(0);

        verify(jpaRepository).countOrdersByStoreIdAndServiceDate(storeId, serviceDate);
        verify(jpaRepository, never()).countOrdersByStoreId(any());
    }

    @Test
    @DisplayName("countByStoreIdAndServiceDate: lanza IllegalArgumentException si storeId es nulo")
    void shouldThrowExceptionWhenStoreIdIsNullInAdapter() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.countByStoreIdAndServiceDate(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El identificador de la tienda es obligatorio.");

        verifyNoInteractions(jpaRepository);
    }

    @Test
    @DisplayName("countByStoreIdAndServiceDate: propaga IllegalStateException si la proyección de agregación es inesperadamente nula")
    void shouldThrowIllegalStateExceptionWhenProjectionIsNull() {
        when(jpaRepository.countOrdersByStoreId(storeId)).thenReturn(null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.countByStoreIdAndServiceDate(storeId, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("la consulta de agregación de pedidos retornó un resultado nulo inesperado");

        verify(jpaRepository).countOrdersByStoreId(storeId);
    }
}
