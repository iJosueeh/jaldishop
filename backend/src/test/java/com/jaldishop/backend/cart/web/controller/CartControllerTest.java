package com.jaldishop.backend.cart.web.controller;

import com.jaldishop.backend.cart.application.AddItemToCartCommand;
import com.jaldishop.backend.cart.application.CartItemView;
import com.jaldishop.backend.cart.application.CartService;
import com.jaldishop.backend.cart.application.CartView;
import com.jaldishop.backend.cart.application.UpdateCartItemQuantityCommand;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;
    private UUID testUserId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        CartController controller = new CartController(cartService);

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
    @DisplayName("GET /api/v1/cart - Retorna 200 OK con el estado del carrito")
    void shouldGetCart() throws Exception {
        UUID cartId = UUID.randomUUID();
        CartView cartView = new CartView(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );

        when(cartService.getCart(testUserId, testStoreId)).thenReturn(cartView);

        mockMvc.perform(get("/api/v1/cart")
                        .param("storeId", testStoreId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cartId.toString()))
                .andExpect(jsonPath("$.userId").value(testUserId.toString()))
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - Retorna 201 Created al agregar ítem")
    void shouldAddItemToCart() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CartItemView itemView = new CartItemView(
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
                true,
                true
        );

        CartView cartView = new CartView(
                cartId,
                testUserId,
                testStoreId,
                List.of(itemView),
                2,
                new BigDecimal("30.00"),
                "PEN",
                Instant.now()
        );

        when(cartService.addItemToCart(eq(testUserId), any(AddItemToCartCommand.class))).thenReturn(cartView);

        String json = """
                {
                    "storeId": "%s",
                    "variantId": "%s",
                    "quantity": 2
                }
                """.formatted(testStoreId, variantId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.totalAmount").value(30.00))
                .andExpect(jsonPath("$.items[0].productName").value("Alfajores Artesanales"))
                .andExpect(jsonPath("$.items[0].presentationName").value("Caja x6"));
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{variantId} - Retorna 200 OK al actualizar cantidad")
    void shouldUpdateCartItemQuantity() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        CartView cartView = new CartView(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                3,
                new BigDecimal("45.00"),
                "PEN",
                Instant.now()
        );

        when(cartService.updateCartItemQuantity(eq(testUserId), any(UpdateCartItemQuantityCommand.class))).thenReturn(cartView);

        String json = """
                {
                    "storeId": "%s",
                    "quantity": 3
                }
                """.formatted(testStoreId);

        mockMvc.perform(put("/api/v1/cart/items/{variantId}", variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalAmount").value(45.00));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/items/{variantId} - Retorna 200 OK al eliminar ítem")
    void shouldRemoveCartItem() throws Exception {
        UUID cartId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();

        CartView cartView = new CartView(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );

        when(cartService.removeCartItem(testUserId, testStoreId, variantId)).thenReturn(cartView);

        mockMvc.perform(delete("/api/v1/cart/items/{variantId}", variantId)
                        .param("storeId", testStoreId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart - Retorna 200 OK al vaciar carrito")
    void shouldClearCart() throws Exception {
        UUID cartId = UUID.randomUUID();

        CartView cartView = new CartView(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );

        when(cartService.clearCart(testUserId, testStoreId)).thenReturn(cartView);

        mockMvc.perform(delete("/api/v1/cart")
                        .param("storeId", testStoreId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }
}
