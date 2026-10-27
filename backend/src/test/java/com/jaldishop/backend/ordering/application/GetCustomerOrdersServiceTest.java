package com.jaldishop.backend.ordering.application;

import com.jaldishop.backend.ordering.domain.*;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderSummaryResponse;
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
class GetCustomerOrdersServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreRepository storeRepository;

    private GetCustomerOrdersService service;

    private UUID customerUserId;
    private UUID storeId;
    private Order testOrder;
    private Store testStore;

    @BeforeEach
    void setUp() {
        service = new GetCustomerOrdersService(orderRepository, storeRepository);
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
    @DisplayName("Retorna la lista de pedidos del cliente con los datos de tienda ensamblados")
    void shouldReturnCustomerOrders() {
        when(orderRepository.findByUserId(customerUserId)).thenReturn(List.of(testOrder));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(testStore));

        List<CustomerOrderSummaryResponse> result = service.execute(customerUserId);

        assertThat(result).hasSize(1);
        CustomerOrderSummaryResponse summary = result.get(0);
        assertThat(summary.orderNumber()).isEqualTo("ORD-00000001");
        assertThat(summary.storeName()).isEqualTo("Panadería Don Pepe");
        assertThat(summary.storeSlug()).isEqualTo("panaderia-don-pepe");
        assertThat(summary.totalAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(summary.totalItems()).isEqualTo(2);
        assertThat(summary.status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Lanza IllegalArgumentException si el userId es nulo")
    void shouldThrowWhenUserIdIsNull() {
        assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El identificador del cliente es obligatorio.");
    }

}
