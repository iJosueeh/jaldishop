package com.jaldishop.backend.review.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaldishop.backend.review.application.CheckReviewEligibilityService;
import com.jaldishop.backend.review.application.CreateProductReviewService;
import com.jaldishop.backend.review.application.GetCustomerReviewsService;
import com.jaldishop.backend.review.application.UpdateProductReviewService;
import com.jaldishop.backend.review.web.dto.CreateReviewRequest;
import com.jaldishop.backend.review.web.dto.CustomerReviewResponse;
import com.jaldishop.backend.review.web.dto.ReviewEligibilityResponse;
import com.jaldishop.backend.review.web.dto.UpdateReviewRequest;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerReviewControllerTest {

    @Mock
    private CheckReviewEligibilityService checkReviewEligibilityService;

    @Mock
    private CreateProductReviewService createProductReviewService;

    @Mock
    private UpdateProductReviewService updateProductReviewService;

    @Mock
    private GetCustomerReviewsService getCustomerReviewsService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID customerUserId;
    private JwtPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        customerUserId = UUID.randomUUID();
        customerPrincipal = new JwtPrincipal(customerUserId, Set.of("CUSTOMER"));
        objectMapper = new ObjectMapper();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(JwtPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return customerPrincipal;
            }
        };

        CustomerReviewController controller = new CustomerReviewController(
                checkReviewEligibilityService,
                createProductReviewService,
                updateProductReviewService,
                getCustomerReviewsService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/customer/products/{productId}/reviews/eligibility retorna 200 con estado")
    void shouldCheckEligibilitySuccessfully() throws Exception {
        UUID productId = UUID.randomUUID();
        ReviewEligibilityResponse response = new ReviewEligibilityResponse(productId, true, false, null);

        when(checkReviewEligibilityService.execute(customerUserId, productId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/customer/products/{productId}/reviews/eligibility", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.alreadyReviewed").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/customer/products/{productId}/reviews crea reseña y retorna 201 Created")
    void shouldCreateReviewSuccessfully() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(5, "Gran calidad.");
        CustomerReviewResponse response = new CustomerReviewResponse(
                UUID.randomUUID(), productId, "Mueble de madera", "mueble-madera",
                "Carpintería Don Luis", "carpinteria-don-luis", null, 5, "Gran calidad.",
                "PUBLISHED", Instant.now(), Instant.now()
        );

        when(createProductReviewService.execute(eq(customerUserId), eq(productId), any(CreateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/customer/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Gran calidad."));
    }

    @Test
    @DisplayName("POST /api/v1/customer/products/{productId}/reviews retorna 422 si no tiene compra verificada")
    void shouldReturn422WhenNotEligible() throws Exception {
        UUID productId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(5, "Gran calidad.");

        when(createProductReviewService.execute(eq(customerUserId), eq(productId), any(CreateReviewRequest.class)))
                .thenThrow(new BusinessRuleException("Solo puedes calificar productos que hayas adquirido en un pedido completado."));

        mockMvc.perform(post("/api/v1/customer/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PUT /api/v1/customer/products/{productId}/reviews actualiza reseña y retorna 200 OK")
    void shouldUpdateReviewSuccessfully() throws Exception {
        UUID productId = UUID.randomUUID();
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Actualizado.");
        CustomerReviewResponse response = new CustomerReviewResponse(
                UUID.randomUUID(), productId, "Mueble de madera", "mueble-madera",
                "Carpintería Don Luis", "carpinteria-don-luis", null, 4, "Actualizado.",
                "PUBLISHED", Instant.now(), Instant.now()
        );

        when(updateProductReviewService.execute(eq(customerUserId), eq(productId), any(UpdateReviewRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/customer/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Actualizado."));
    }

    @Test
    @DisplayName("GET /api/v1/customer/reviews retorna la lista de reseñas del cliente autenticado")
    void shouldGetCustomerReviewsList() throws Exception {
        UUID productId = UUID.randomUUID();
        CustomerReviewResponse response = new CustomerReviewResponse(
                UUID.randomUUID(), productId, "Mueble de madera", "mueble-madera",
                "Carpintería Don Luis", "carpinteria-don-luis", null, 5, "Excelente.",
                "PUBLISHED", Instant.now(), Instant.now()
        );

        when(getCustomerReviewsService.execute(customerUserId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/customer/reviews")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productName").value("Mueble de madera"));
    }
}
