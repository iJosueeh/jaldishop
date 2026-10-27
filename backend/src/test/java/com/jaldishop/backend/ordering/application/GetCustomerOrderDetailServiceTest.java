package com.jaldishop.backend.ordering.application;

import com.jaldishop.backend.ordering.domain.*;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderDetailResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCustomerOrderDetailServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    private GetCustomerOrderDetailService service;

    private UUID customerUserId;
    private UUID storeId;
    private Order testOrder;
    private Store testStore;

    @BeforeEach
    void setUp() {
        service = new GetCustomerOrderDetailService(orderRepository, storeRepository);
        customerUserId = UUID.randomUUID();
        storeId = UUID.randomUUID();

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

        Order.CreateData createData = new Order.CreateData(
                customerUserId,
                storeId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                DeliveryMode.PICKUP,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Juan Pérez",
                "987654321",
                "juan@example.com",
                null,
                null,
                null,
                null,
                "PEN",
                new BigDecimal("20.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("20.00"),
                List.of(new Order.ItemData(UUID.randomUUID(), "Pan Francés", "Bolsa x10", 2, new BigDecimal("10.00")))
        );

        testOrder = Order.createConfirmed(createData, "ORD-00000001");
    }

    @Test
    @DisplayName("Retorna el detalle completo de la orden si pertenece al cliente autenticado")
    void shouldReturnOrderDetailWhenBelongsToCustomer() {
        when(orderRepository.findByOrderNumber("ORD-00000001")).thenReturn(Optional.of(testOrder));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

        CustomerOrderDetailResponse result = service.execute(customerUserId, "ORD-00000001");

        assertThat(result.orderNumber()).isEqualTo("ORD-00000001");
        assertThat(result.store().name()).isEqualTo("Panadería Don Pepe");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productName()).isEqualTo("Pan Francés");
        assertThat(result.statusHistory()).hasSize(1);
        assertThat(result.statusHistory().get(0).status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el pedido pertenece a otro usuario (prevención IDOR)")
    void shouldThrowNotFoundWhenOrderBelongsToAnotherCustomer() {
        UUID anotherUser = UUID.randomUUID();
        when(orderRepository.findByOrderNumber("ORD-00000001")).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> service.execute(anotherUser, "ORD-00000001"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el número de pedido no existe")
    void shouldThrowNotFoundWhenOrderDoesNotExist() {
        when(orderRepository.findByOrderNumber("ORD-99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(customerUserId, "ORD-99999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

}
