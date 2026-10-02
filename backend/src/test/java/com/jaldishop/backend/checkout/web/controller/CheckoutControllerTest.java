package com.jaldishop.backend.checkout.web.controller;

import com.jaldishop.backend.checkout.application.CheckoutResult;
import com.jaldishop.backend.checkout.application.CheckoutService;
import com.jaldishop.backend.checkout.application.InitiateCheckoutCommand;
import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.CheckoutPricing;
import com.jaldishop.backend.checkout.web.dto.CheckoutCustomerResponse;
import com.jaldishop.backend.checkout.web.dto.CheckoutItemResponse;
import com.jaldishop.backend.checkout.web.dto.CheckoutPricingResponse;
import com.jaldishop.backend.checkout.web.dto.CheckoutResponse;
import com.jaldishop.backend.checkout.web.mapper.CheckoutResponseMapper;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CheckoutControllerTest {

    @Mock
    private CheckoutService checkoutService;

    @Mock
    private CheckoutResponseMapper responseMapper;

    private MockMvc mockMvc;
    private UUID testUserId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        CheckoutController controller = new CheckoutController(checkoutService, responseMapper);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/checkout - Retorna 201 Created al iniciar checkout exitosamente")
    void shouldInitiateCheckoutSuccessfully() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDate serviceDate = LocalDate.now().plusDays(1);
        Instant now = Instant.now();

        CheckoutItemSnapshot itemSnapshot = new CheckoutItemSnapshot(
                variantId,
                productId,
                "Alfajores Artesanales",
                "Caja x6",
                "ALF-06",
                "https://example.com/alfajor.png",
                2,
                new BigDecimal("15.00"),
                "PEN",
                new BigDecimal("30.00"),
                true
        );

        CheckoutPricing pricing = new CheckoutPricing(
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                new BigDecimal("18.00"),
                new BigDecimal("4.58"),
                new BigDecimal("30.00"),
                "PEN"
        );

        CheckoutResult result = new CheckoutResult(
                reservationId,
                now.plusSeconds(600),
                testStoreId,
                "Dulcería Doña Rosa",
                testUserId,
                CheckoutFulfillmentType.PICKUP,
                serviceDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                "Carlos Vega",
                "987654321",
                "carlos@example.com",
                null,
                null,
                List.of(itemSnapshot),
                pricing,
                now
        );

        CheckoutItemResponse itemResponse = new CheckoutItemResponse(
                variantId,
                productId,
                "Alfajores Artesanales",
                "Caja x6",
                "ALF-06",
                "https://example.com/alfajor.png",
                2,
                new BigDecimal("15.00"),
                "PEN",
                new BigDecimal("30.00"),
                true
        );

        CheckoutPricingResponse pricingResponse = new CheckoutPricingResponse(
                new BigDecimal("30.00"),
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                new BigDecimal("18.00"),
                new BigDecimal("4.58"),
                new BigDecimal("30.00"),
                "PEN"
        );

        CheckoutResponse response = new CheckoutResponse(
                reservationId,
                now.plusSeconds(600),
                testStoreId,
                "Dulcería Doña Rosa",
                testUserId,
                CheckoutFulfillmentType.PICKUP,
                serviceDate,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                new CheckoutCustomerResponse("Carlos Vega", "987654321", "carlos@example.com", null, null),
                List.of(itemResponse),
                pricingResponse,
                now
        );

        when(checkoutService.initiateCheckout(any(InitiateCheckoutCommand.class))).thenReturn(result);
        when(responseMapper.toResponse(result)).thenReturn(response);

        String json = """
                {
                    "storeId": "%s",
                    "fulfillmentType": "PICKUP",
                    "serviceDate": "%s",
                    "startTime": "10:00:00",
                    "endTime": "12:00:00",
                    "customerName": "Carlos Vega",
                    "customerPhone": "987654321",
                    "customerEmail": "carlos@example.com"
                }
                """.formatted(testStoreId, serviceDate);

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId.toString()))
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.storeName").value("Dulcería Doña Rosa"))
                .andExpect(jsonPath("$.fulfillmentType").value("PICKUP"))
                .andExpect(jsonPath("$.pricing.totalAmount").value(30.00))
                .andExpect(jsonPath("$.items[0].productName").value("Alfajores Artesanales"));
    }

    @Test
    @DisplayName("POST /api/v1/checkout - Retorna 400 Bad Request si faltan campos obligatorios")
    void shouldReturn400WhenValidationFails() throws Exception {
        String json = "{}";

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/checkout - Retorna 409 Conflict si el carrito está vacío")
    void shouldReturn409WhenCartIsEmpty() throws Exception {
        when(checkoutService.initiateCheckout(any(InitiateCheckoutCommand.class)))
                .thenThrow(new ConflictException("CART_EMPTY", "El carrito de compras se encuentra vacío."));

        String json = """
                {
                    "storeId": "%s",
                    "fulfillmentType": "PICKUP",
                    "serviceDate": "%s"
                }
                """.formatted(testStoreId, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CART_EMPTY"));
    }

    @Test
    @DisplayName("POST /api/v1/checkout - Retorna 409 Conflict si la capacidad está agotada")
    void shouldReturn409WhenCapacityExhausted() throws Exception {
        when(checkoutService.initiateCheckout(any(InitiateCheckoutCommand.class)))
                .thenThrow(new ConflictException("CAPACITY_EXHAUSTED", "No quedan cupos disponibles."));

        String json = """
                {
                    "storeId": "%s",
                    "fulfillmentType": "DELIVERY",
                    "serviceDate": "%s",
                    "deliveryAddress": "Av Principal 123"
                }
                """.formatted(testStoreId, LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/v1/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXHAUSTED"));
    }
}
