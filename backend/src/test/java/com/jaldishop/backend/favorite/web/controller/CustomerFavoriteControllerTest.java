package com.jaldishop.backend.favorite.web.controller;

import com.jaldishop.backend.favorite.application.AddFavoriteProductService;
import com.jaldishop.backend.favorite.application.CheckFavoriteProductService;
import com.jaldishop.backend.favorite.application.GetCustomerFavoriteProductsService;
import com.jaldishop.backend.favorite.application.RemoveFavoriteProductService;
import com.jaldishop.backend.favorite.web.dto.FavoriteProductResponse;
import com.jaldishop.backend.favorite.web.dto.FavoriteStatusResponse;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerFavoriteControllerTest {

    @Mock
    private AddFavoriteProductService addFavoriteProductService;

    @Mock
    private RemoveFavoriteProductService removeFavoriteProductService;

    @Mock
    private CheckFavoriteProductService checkFavoriteProductService;

    @Mock
    private GetCustomerFavoriteProductsService getCustomerFavoriteProductsService;

    private MockMvc mockMvc;
    private UUID customerUserId;
    private JwtPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        customerUserId = UUID.randomUUID();
        customerPrincipal = new JwtPrincipal(customerUserId, Set.of("CUSTOMER"));

        CustomerFavoriteController controller = new CustomerFavoriteController(
                addFavoriteProductService,
                removeFavoriteProductService,
                checkFavoriteProductService,
                getCustomerFavoriteProductsService
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
    @DisplayName("GET /api/v1/customer/favorites/products retorna lista de favoritos")
    void shouldReturnFavoriteProducts() throws Exception {
        UUID productId = UUID.randomUUID();
        FavoriteProductResponse item = new FavoriteProductResponse(
                productId,
                "Croissant de Mantequilla",
                "croissant-mantequilla",
                "https://images.jaldishop.com/products/croissant.jpg",
                UUID.randomUUID(),
                "Panadería Don Pepe",
                "panaderia-don-pepe",
                new BigDecimal("6.50"),
                "PEN",
                true,
                Instant.parse("2026-10-25T10:00:00Z")
        );

        when(getCustomerFavoriteProductsService.execute(customerUserId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/customer/favorites/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$[0].productName").value("Croissant de Mantequilla"))
                .andExpect(jsonPath("$[0].storeName").value("Panadería Don Pepe"))
                .andExpect(jsonPath("$[0].minPrice").value(6.50))
                .andExpect(jsonPath("$[0].isAvailable").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/customer/favorites/products/{productId} agrega un favorito")
    void shouldAddFavoriteProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        when(addFavoriteProductService.execute(customerUserId, productId))
                .thenReturn(new FavoriteStatusResponse(productId, true));

        mockMvc.perform(post("/api/v1/customer/favorites/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.isFavorite").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/customer/favorites/products/{productId} remueve un favorito")
    void shouldRemoveFavoriteProduct() throws Exception {
        UUID productId = UUID.randomUUID();
        when(removeFavoriteProductService.execute(customerUserId, productId))
                .thenReturn(new FavoriteStatusResponse(productId, false));

        mockMvc.perform(delete("/api/v1/customer/favorites/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.isFavorite").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/customer/favorites/products/{productId}/status consulta si es favorito")
    void shouldCheckFavoriteStatus() throws Exception {
        UUID productId = UUID.randomUUID();
        when(checkFavoriteProductService.execute(customerUserId, productId))
                .thenReturn(new FavoriteStatusResponse(productId, true));

        mockMvc.perform(get("/api/v1/customer/favorites/products/" + productId + "/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.isFavorite").value(true));
    }
}
