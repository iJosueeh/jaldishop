package com.jaldishop.backend.ordering.application.service;

import com.jaldishop.backend.ordering.application.result.MerchantOrderStatusCountsResponse;
import com.jaldishop.backend.ordering.domain.model.OrderStatusCounts;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMerchantOrderStatusCountsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private GetMerchantOrderStatusCountsService service;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        service = new GetMerchantOrderStatusCountsService(orderRepository);
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Debe fallar con IllegalArgumentException cuando storeId es nulo")
    void shouldThrowExceptionWhenStoreIdIsNull() {
        assertThatThrownBy(() -> service.execute(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El identificador de la tienda es obligatorio.");

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("Debe delegar correctamente al repositorio sin fecha para histórico")
    void shouldDelegateToRepositoryWithoutDateForHistoricalCounts() {
        OrderStatusCounts domainCounts = OrderStatusCounts.of(12, 6, 4, 2, 20, 4);
        when(orderRepository.countByStoreIdAndServiceDate(storeId, null)).thenReturn(domainCounts);

        MerchantOrderStatusCountsResponse response = service.execute(storeId, null);

        assertThat(response.all()).isEqualTo(48);
        assertThat(response.confirmed()).isEqualTo(12);
        assertThat(response.inPreparation()).isEqualTo(6);
        assertThat(response.ready()).isEqualTo(4);
        assertThat(response.outForDelivery()).isEqualTo(2);
        assertThat(response.completed()).isEqualTo(20);
        assertThat(response.cancelled()).isEqualTo(4);

        verify(orderRepository).countByStoreIdAndServiceDate(storeId, null);
    }

    @Test
    @DisplayName("Debe delegar correctamente al repositorio con fecha operativa")
    void shouldDelegateToRepositoryWithDateForDailyCounts() {
        LocalDate serviceDate = LocalDate.of(2026, 11, 14);
        OrderStatusCounts domainCounts = OrderStatusCounts.of(4, 3, 2, 0, 1, 0);
        when(orderRepository.countByStoreIdAndServiceDate(storeId, serviceDate)).thenReturn(domainCounts);

        MerchantOrderStatusCountsResponse response = service.execute(storeId, serviceDate);

        assertThat(response.all()).isEqualTo(10);
        assertThat(response.confirmed()).isEqualTo(4);
        assertThat(response.inPreparation()).isEqualTo(3);
        assertThat(response.ready()).isEqualTo(2);
        assertThat(response.outForDelivery()).isEqualTo(0);
        assertThat(response.completed()).isEqualTo(1);
        assertThat(response.cancelled()).isEqualTo(0);

        verify(orderRepository).countByStoreIdAndServiceDate(storeId, serviceDate);
    }

    @Test
    @DisplayName("execute(): retorna contadores en cero cuando el repositorio devuelve OrderStatusCounts.empty()")
    void shouldReturnZerosWhenRepositoryReturnsEmptyCounts() {
        when(orderRepository.countByStoreIdAndServiceDate(storeId, null)).thenReturn(OrderStatusCounts.empty());

        MerchantOrderStatusCountsResponse response = service.execute(storeId, null);

        assertThat(response.all()).isZero();
        assertThat(response.confirmed()).isZero();
        assertThat(response.inPreparation()).isZero();
        assertThat(response.ready()).isZero();
        assertThat(response.outForDelivery()).isZero();
        assertThat(response.completed()).isZero();
        assertThat(response.cancelled()).isZero();

        verify(orderRepository).countByStoreIdAndServiceDate(storeId, null);
    }

    @Test
    @DisplayName("execute(): propaga NullPointerException si el repositorio retorna null inesperadamente")
    void shouldThrowExceptionWhenRepositoryReturnsNull() {
        when(orderRepository.countByStoreIdAndServiceDate(storeId, null)).thenReturn(null);

        assertThatThrownBy(() -> service.execute(storeId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("El objeto OrderStatusCounts no puede ser nulo.");

        verify(orderRepository).countByStoreIdAndServiceDate(storeId, null);
    }

    @Test
    @DisplayName("Debe propagar excepciones del repositorio sin convertir silenciosamente a ceros")
    void shouldPropagateRepositoryExceptionsWithoutSwallowing() {
        when(orderRepository.countByStoreIdAndServiceDate(storeId, null))
                .thenThrow(new RuntimeException("Error crítico de conexión con PostgreSQL"));

        assertThatThrownBy(() -> service.execute(storeId, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error crítico de conexión con PostgreSQL");
    }
}
