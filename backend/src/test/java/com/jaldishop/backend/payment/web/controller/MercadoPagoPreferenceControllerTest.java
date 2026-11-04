package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceCommand;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceResult;
import com.jaldishop.backend.payment.application.preference.CreatePaymentPreferenceService;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
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

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MercadoPagoPreferenceControllerTest {

    @Mock
    private CreatePaymentPreferenceService service;

    private MockMvc mockMvc;
    private final UUID authenticatedUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MercadoPagoPreferenceController controller = new MercadoPagoPreferenceController(service);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && JwtPrincipal.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new JwtPrincipal(authenticatedUserId, Set.of("CUSTOMER"));
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /preference devuelve 201 Created con paymentId, preferenceId e initPoint")
    void shouldCreatePreferenceSuccessfully() throws Exception {
        UUID reservationId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(service.execute(any(CreatePaymentPreferenceCommand.class))).thenReturn(
                new CreatePaymentPreferenceResult(paymentId, "pref-999888", "https://mercadopago.com/init/999888", "PENDING")
        );

        String requestBody = """
                {
                  "capacityReservationId": "%s",
                  "fulfillmentType": "DELIVERY",
                  "customerName": "Carlos Mendoza",
                  "customerPhone": "+51987654321",
                  "customerEmail": "carlos@example.com",
                  "deliveryAddress": "Av. Brasil 500",
                  "deliveryReference": "Piso 4"
                }
                """.formatted(reservationId);

        mockMvc.perform(post("/api/v1/checkout/mercado-pago/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.preferenceId").value("pref-999888"))
                .andExpect(jsonPath("$.initPoint").value("https://mercadopago.com/init/999888"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /preference rechaza cuerpo con campos obligatorios faltantes con HTTP 400")
    void shouldRejectInvalidRequestBody() throws Exception {
        String invalidBody = """
                {
                  "fulfillmentType": "DELIVERY",
                  "customerName": "",
                  "customerEmail": "not-an-email"
                }
                """;

        mockMvc.perform(post("/api/v1/checkout/mercado-pago/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /preference propaga excepciones de dominio con formato de error controlado")
    void shouldHandleDomainConflictException() throws Exception {
        UUID reservationId = UUID.randomUUID();

        when(service.execute(any(CreatePaymentPreferenceCommand.class)))
                .thenThrow(new ConflictException("RESERVATION_EXPIRED", "La reserva de capacidad se encuentra expirada."));

        String requestBody = """
                {
                  "capacityReservationId": "%s",
                  "fulfillmentType": "PICKUP",
                  "customerName": "Carlos Mendoza",
                  "customerPhone": "+51987654321",
                  "customerEmail": "carlos@example.com"
                }
                """.formatted(reservationId);

        mockMvc.perform(post("/api/v1/checkout/mercado-pago/preference")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_EXPIRED"))
                .andExpect(jsonPath("$.message").value("La reserva de capacidad se encuentra expirada."));
    }

}
