package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.status.GetCustomerPaymentStatusService;
import com.jaldishop.backend.payment.web.dto.CustomerPaymentStatusResponse;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CustomerPaymentStatusControllerTest {

    @Mock
    private GetCustomerPaymentStatusService service;

    private MockMvc mockMvc;
    private final UUID authenticatedUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        CustomerPaymentStatusController controller = new CustomerPaymentStatusController(service);

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
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /status retorna 200 OK con el estado del pago")
    void returnsOkWithPaymentStatus() throws Exception {
        UUID paymentId = UUID.randomUUID();
        CustomerPaymentStatusResponse mockResponse = new CustomerPaymentStatusResponse(
                paymentId,
                "APPROVED",
                true,
                "ORD-12345",
                "CONFIRMED",
                false,
                null,
                null,
                "NOT_REQUIRED",
                new BigDecimal("45.00"),
                "PEN",
                Instant.now(),
                Instant.now()
        );

        when(service.execute(paymentId, authenticatedUserId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/customer/payments/{paymentId}/status", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.paymentStatus").value("APPROVED"))
                .andExpect(jsonPath("$.orderCreated").value(true))
                .andExpect(jsonPath("$.orderNumber").value("ORD-12345"));
    }

    @Test
    @DisplayName("GET /status retorna 404 cuando el pago no existe")
    void returns404WhenPaymentNotFound() throws Exception {
        UUID paymentId = UUID.randomUUID();

        when(service.execute(paymentId, authenticatedUserId))
                .thenThrow(new ResourceNotFoundException("Pago no encontrado: " + paymentId));

        mockMvc.perform(get("/api/v1/customer/payments/{paymentId}/status", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Pago no encontrado: " + paymentId));
    }

    @Test
    @DisplayName("GET /status retorna 403 cuando el pago pertenece a otro usuario")
    void returns403WhenNotAuthorized() throws Exception {
        UUID paymentId = UUID.randomUUID();

        when(service.execute(paymentId, authenticatedUserId))
                .thenThrow(new AccessDeniedException("No tienes autorización para consultar este pago."));

        mockMvc.perform(get("/api/v1/customer/payments/{paymentId}/status", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /status retorna 403 cuando el usuario no tiene rol CUSTOMER")
    void returns403WhenRoleIsNotCustomer() throws Exception {
        UUID paymentId = UUID.randomUUID();
        CustomerPaymentStatusController controller = new CustomerPaymentStatusController(service);

        HandlerMethodArgumentResolver merchantResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && JwtPrincipal.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new JwtPrincipal(authenticatedUserId, Set.of("MERCHANT"));
            }
        };

        MockMvc merchantMockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(merchantResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        merchantMockMvc.perform(get("/api/v1/customer/payments/{paymentId}/status", paymentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Solo los usuarios con el rol CUSTOMER pueden consultar el estado de sus pagos."));
    }
}
