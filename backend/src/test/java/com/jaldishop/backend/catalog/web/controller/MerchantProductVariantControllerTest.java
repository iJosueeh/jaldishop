package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductVariantCommand;
import com.jaldishop.backend.catalog.application.ProductVariantService;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantProductVariantControllerTest {

    @Mock
    private ProductVariantService variantService;

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private JwtPrincipal mockPrincipal;

    private MockMvc mockMvc;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        MerchantProductVariantController controller = new MerchantProductVariantController(variantService, storeContextService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        auth = new UsernamePasswordAuthenticationToken(mockPrincipal, null, Collections.emptyList());
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 201 Created")
    void shouldCreateVariant() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductVariant variant = ProductVariant.create(
                productId,
                "Porción Individual",
                "CHEESE-IND-01",
                new BigDecimal("18.50"),
                "PEN",
                true,
                Collections.emptyList()
        );

        lenient().doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        lenient().when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(variantService.createVariant(any(CreateProductVariantCommand.class))).thenReturn(variant);

        String requestJson = """
                {
                    "presentationName": "Porción Individual",
                    "sku": "CHEESE-IND-01",
                    "priceAmount": 18.50,
                    "priceCurrency": "PEN",
                    "tracksInventory": true,
                    "attributes": []
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.presentationName").value("Porción Individual"))
                .andExpect(jsonPath("$.sku").value("CHEESE-IND-01"))
                .andExpect(jsonPath("$.priceCurrency").value("PEN"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 400 Bad Request cuando moneda no es 3 letras mayúsculas")
    void shouldRejectInvalidCurrency() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        lenient().doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        lenient().when(storeContextService.requireStoreId(any())).thenReturn(storeId);

        String requestJson = """
                {
                    "presentationName": "Porción Individual",
                    "sku": "CHEESE-IND-01",
                    "priceAmount": 18.50,
                    "priceCurrency": "123",
                    "tracksInventory": true,
                    "attributes": []
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 200 OK")
    void shouldGetVariants() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductVariant variant = ProductVariant.create(
                productId,
                "Porción Grande",
                "CHEESE-GRD-01",
                new BigDecimal("45.00"),
                "PEN",
                false,
                Collections.emptyList()
        );

        lenient().doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        lenient().when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(variantService.getVariantsByProduct(productId, storeId)).thenReturn(List.of(variant));

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId)
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presentationName").value("Porción Grande"));
    }
}