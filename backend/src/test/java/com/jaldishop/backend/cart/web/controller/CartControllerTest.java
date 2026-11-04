package com.jaldishop.backend.cart.web.controller;
import com.jaldishop.backend.cart.domain.model.*;
import com.jaldishop.backend.cart.domain.repository.*;
import com.jaldishop.backend.cart.application.command.*;
import com.jaldishop.backend.cart.application.query.*;
import com.jaldishop.backend.cart.application.result.*;
import com.jaldishop.backend.cart.application.service.*;


import com.jaldishop.backend.cart.application.command.AddItemToCartCommand;
import com.jaldishop.backend.cart.application.service.AddItemToCartService;
import com.jaldishop.backend.cart.application.result.CartItemView;
import com.jaldishop.backend.cart.application.result.CartView;
import com.jaldishop.backend.cart.application.command.ClearCartCommand;
import com.jaldishop.backend.cart.application.service.ClearCartService;
import com.jaldishop.backend.cart.application.query.GetCartQuery;
import com.jaldishop.backend.cart.application.service.GetCartService;
import com.jaldishop.backend.cart.application.command.RemoveCartItemCommand;
import com.jaldishop.backend.cart.application.service.RemoveCartItemService;
import com.jaldishop.backend.cart.application.command.UpdateCartItemQuantityCommand;
import com.jaldishop.backend.cart.application.service.UpdateCartItemQuantityService;
import com.jaldishop.backend.cart.web.dto.CartItemResponse;
import com.jaldishop.backend.cart.web.dto.CartResponse;
import com.jaldishop.backend.cart.web.mapper.CartResponseMapper;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private GetCartService getCartService;

    @Mock
    private AddItemToCartService addItemToCartService;

    @Mock
    private UpdateCartItemQuantityService updateCartItemQuantityService;

    @Mock
    private RemoveCartItemService removeCartItemService;

    @Mock
    private ClearCartService clearCartService;

    @Mock
    private CartResponseMapper responseMapper;

    private MockMvc mockMvc;
    private UUID testUserId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        CartController controller = new CartController(
                getCartService,
                addItemToCartService,
                updateCartItemQuantityService,
                removeCartItemService,
                clearCartService,
                responseMapper
        );

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
        CartResponse response = new CartResponse(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );

        when(getCartService.execute(any(GetCartQuery.class))).thenReturn(cartView);
        when(responseMapper.toResponse(cartView)).thenReturn(response);

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

        CartItemResponse itemResponse = new CartItemResponse(
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

        CartResponse response = new CartResponse(
                cartId,
                testUserId,
                testStoreId,
                List.of(itemResponse),
                2,
                new BigDecimal("30.00"),
                "PEN",
                Instant.now()
        );

        when(addItemToCartService.execute(any(AddItemToCartCommand.class))).thenReturn(cartView);
        when(responseMapper.toResponse(cartView)).thenReturn(response);

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

        CartResponse response = new CartResponse(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                3,
                new BigDecimal("45.00"),
                "PEN",
                Instant.now()
        );

        when(updateCartItemQuantityService.execute(any(UpdateCartItemQuantityCommand.class))).thenReturn(cartView);
        when(responseMapper.toResponse(cartView)).thenReturn(response);

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

        CartResponse response = new CartResponse(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );

        when(removeCartItemService.execute(any(RemoveCartItemCommand.class))).thenReturn(cartView);
        when(responseMapper.toResponse(cartView)).thenReturn(response);

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

        CartResponse response = new CartResponse(
                cartId,
                testUserId,
                testStoreId,
                List.of(),
                0,
                BigDecimal.ZERO,
                "PEN",
                Instant.now()
        );

        when(clearCartService.execute(any(ClearCartCommand.class))).thenReturn(cartView);
        when(responseMapper.toResponse(cartView)).thenReturn(response);

        mockMvc.perform(delete("/api/v1/cart")
                        .param("storeId", testStoreId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - 400 VALIDATION_ERROR cuando la cantidad es menor a 1")
    void shouldRejectAddItemWithZeroQuantity() throws Exception {
        UUID variantId = UUID.randomUUID();
        String json = """
                {
                    "storeId": "%s",
                    "variantId": "%s",
                    "quantity": 0
                }
                """.formatted(testStoreId, variantId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.quantity").value("La cantidad debe ser al menos 1"));

        verifyNoInteractions(addItemToCartService);
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - 400 VALIDATION_ERROR cuando falta storeId")
    void shouldRejectAddItemWithMissingStoreId() throws Exception {
        UUID variantId = UUID.randomUUID();
        String json = """
                {
                    "variantId": "%s",
                    "quantity": 2
                }
                """.formatted(variantId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.storeId").value("El ID de la tienda es obligatorio"));

        verifyNoInteractions(addItemToCartService);
    }

    @Test
    @DisplayName("PUT /api/v1/cart/items/{variantId} - 400 VALIDATION_ERROR cuando cantidad es negativa")
    void shouldRejectUpdateItemWithNegativeQuantity() throws Exception {
        UUID variantId = UUID.randomUUID();
        String json = """
                {
                    "storeId": "%s",
                    "quantity": -1
                }
                """.formatted(testStoreId);

        mockMvc.perform(put("/api/v1/cart/items/{variantId}", variantId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.quantity").value("La cantidad no puede ser negativa"));

        verifyNoInteractions(updateCartItemQuantityService);
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - 409 Conflict cuando la variante está inactiva")
    void shouldReturn409WhenVariantIsInactive() throws Exception {
        UUID variantId = UUID.randomUUID();
        when(addItemToCartService.execute(any(AddItemToCartCommand.class)))
                .thenThrow(new ConflictException("VARIANT_INACTIVE", "La variante de producto no se encuentra disponible"));

        String json = """
                {
                    "storeId": "%s",
                    "variantId": "%s",
                    "quantity": 1
                }
                """.formatted(testStoreId, variantId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VARIANT_INACTIVE"))
                .andExpect(jsonPath("$.message").value("La variante de producto no se encuentra disponible"));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - 409 Conflict cuando el producto no pertenece a la tienda")
    void shouldReturn409WhenProductStoreMismatch() throws Exception {
        UUID variantId = UUID.randomUUID();
        when(addItemToCartService.execute(any(AddItemToCartCommand.class)))
                .thenThrow(new ConflictException("VARIANT_STORE_MISMATCH", "El producto no pertenece a la tienda especificada"));

        String json = """
                {
                    "storeId": "%s",
                    "variantId": "%s",
                    "quantity": 1
                }
                """.formatted(testStoreId, variantId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VARIANT_STORE_MISMATCH"))
                .andExpect(jsonPath("$.message").value("El producto no pertenece a la tienda especificada"));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - 409 Conflict cuando el producto está inactivo")
    void shouldReturn409WhenProductIsInactive() throws Exception {
        UUID variantId = UUID.randomUUID();
        when(addItemToCartService.execute(any(AddItemToCartCommand.class)))
                .thenThrow(new ConflictException("PRODUCT_INACTIVE", "El producto no se encuentra activo"));

        String json = """
                {
                    "storeId": "%s",
                    "variantId": "%s",
                    "quantity": 1
                }
                """.formatted(testStoreId, variantId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_INACTIVE"))
                .andExpect(jsonPath("$.message").value("El producto no se encuentra activo"));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - 404 Not Found cuando la variante no existe")
    void shouldReturn404WhenVariantNotFound() throws Exception {
        UUID variantId = UUID.randomUUID();
        when(addItemToCartService.execute(any(AddItemToCartCommand.class)))
                .thenThrow(new ResourceNotFoundException("Variante no encontrada"));

        String json = """
                {
                    "storeId": "%s",
                    "variantId": "%s",
                    "quantity": 1
                }
                """.formatted(testStoreId, variantId);

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Variante no encontrada"));
    }

    @Test
    @DisplayName("GET /api/v1/cart - 404 Not Found cuando la tienda no existe")
    void shouldReturn404WhenStoreNotFoundOnGetCart() throws Exception {
        when(getCartService.execute(any(GetCartQuery.class)))
                .thenThrow(new ResourceNotFoundException("Tienda no encontrada"));

        mockMvc.perform(get("/api/v1/cart")
                        .param("storeId", testStoreId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Tienda no encontrada"));
    }
}
