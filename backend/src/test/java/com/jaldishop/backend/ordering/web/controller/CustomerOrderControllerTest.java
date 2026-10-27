package com.jaldishop.backend.ordering.web.controller;

import com.jaldishop.backend.ordering.application.GetCustomerOrderDetailService;
import com.jaldishop.backend.ordering.application.GetCustomerOrdersService;
import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.domain.OrderStatus;
import com.jaldishop.backend.ordering.web.dto.*;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerOrderControllerTest {

    @Mock
    private GetCustomerOrdersService getCustomerOrdersService;

    @Mock
    private GetCustomerOrderDetailService getCustomerOrderDetailService;

    private MockMvc mockMvc;
    private UUID customerUserId;
    private JwtPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        customerUserId = UUID.randomUUID();
        customerPrincipal = new JwtPrincipal(customerUserId, Set.of("CUSTOMER"));

        CustomerOrderController controller = new CustomerOrderController(
                getCustomerOrdersService,
                getCustomerOrderDetailService
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return customerPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/customer/orders retorna lista de pedidos del cliente")
    void shouldReturnCustomerOrders() throws Exception {
        CustomerOrderSummaryResponse summary = new CustomerOrderSummaryResponse(
                UUID.randomUUID(),
                "ORD-20261025-0001",
                UUID.randomUUID(),
                "Bodega San José",
                "bodega-san-jose",
                "https://images.jaldishop.com/stores/san-jose.jpg",
                OrderStatus.CONFIRMED,
                DeliveryMode.DELIVERY,
                LocalDate.of(2026, 10, 25),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                new BigDecimal("45.50"),
                "PEN",
                3,
                Instant.parse("2026-10-25T15:30:00Z")
        );

        when(getCustomerOrdersService.execute(customerUserId)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/customer/orders")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-20261025-0001"))
                .andExpect(jsonPath("$[0].storeName").value("Bodega San José"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$[0].deliveryMode").value("DELIVERY"))
                .andExpect(jsonPath("$[0].totalAmount").value(45.50))
                .andExpect(jsonPath("$[0].totalItems").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/customer/orders/{orderNumber} retorna detalle completo del pedido")
    void shouldReturnOrderDetail() throws Exception {
        CustomerOrderStoreInfo storeInfo = new CustomerOrderStoreInfo(
                UUID.randomUUID(),
                "Bodega San José",
                "bodega-san-jose",
                "987654321",
                "987654321",
                "Av. Primavera 123",
                "Frente al parque",
                "https://images.jaldishop.com/stores/san-jose.jpg"
        );

        CustomerOrderItemResponse item = new CustomerOrderItemResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Arroz Costeño 1kg",
                null,
                2,
                new BigDecimal("5.00"),
                new BigDecimal("10.00")
        );

        CustomerOrderStatusHistoryResponse history = new CustomerOrderStatusHistoryResponse(
                OrderStatus.CONFIRMED,
                "Pedido confirmado por el cliente",
                Instant.parse("2026-10-25T15:30:00Z")
        );

        CustomerOrderDetailResponse detail = new CustomerOrderDetailResponse(
                UUID.randomUUID(),
                "ORD-20261025-0001",
                storeInfo,
                OrderStatus.CONFIRMED,
                DeliveryMode.DELIVERY,
                LocalDate.of(2026, 10, 25),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                "Carlos Gómez",
                "987654321",
                "carlos@gmail.com",
                "Calle Los Sauces 456",
                "Dejar en portería",
                new BigDecimal("-12.046374"),
                new BigDecimal("-77.042793"),
                "PEN",
                new BigDecimal("10.00"),
                BigDecimal.ZERO,
                null,
                new BigDecimal("3.00"),
                BigDecimal.ZERO,
                new BigDecimal("13.00"),
                Instant.parse("2026-10-25T15:30:00Z"),
                Instant.parse("2026-10-25T15:30:00Z"),
                List.of(item),
                List.of(history)
        );

        when(getCustomerOrderDetailService.execute(customerUserId, "ORD-20261025-0001")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/customer/orders/ORD-20261025-0001")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-20261025-0001"))
                .andExpect(jsonPath("$.store.name").value("Bodega San José"))
                .andExpect(jsonPath("$.items[0].productName").value("Arroz Costeño 1kg"))
                .andExpect(jsonPath("$.totalAmount").value(13.00))
                .andExpect(jsonPath("$.statusHistory[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/customer/orders/{orderNumber} retorna 404 si el pedido no existe o es de otro usuario")
    void shouldReturn404WhenNotFoundOrBelongsToAnotherCustomer() throws Exception {
        when(getCustomerOrderDetailService.execute(customerUserId, "ORD-UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Pedido", "ORD-UNKNOWN"));

        mockMvc.perform(get("/api/v1/customer/orders/ORD-UNKNOWN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
