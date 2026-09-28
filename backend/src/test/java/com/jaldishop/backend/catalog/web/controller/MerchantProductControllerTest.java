package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductCommand;
import com.jaldishop.backend.catalog.application.CreateProductService;
import com.jaldishop.backend.catalog.application.GetProductsService;
import com.jaldishop.backend.catalog.application.UpdateProductService;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.mapper.ProductResponseMapper;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantProductControllerTest {

    @Mock
    private CreateProductService createProductService;

    @Mock
    private UpdateProductService updateProductService;

    @Mock
    private GetProductsService getProductsService;

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private ProductResponseMapper responseMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MerchantProductController controller = new MerchantProductController(
                createProductService,
                updateProductService,
                getProductsService,
                storeContextService,
                responseMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products - 201 Created cuando es válido")
    void shouldCreateProduct() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Torta Tres Leches", "tres-leches", "Deliciosa torta", null);
        ProductResponse response = new ProductResponse(
                product.getId(),
                storeId,
                categoryId,
                "Torta Tres Leches",
                "tres-leches",
                "Deliciosa torta",
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(createProductService.execute(any(CreateProductCommand.class))).thenReturn(product);
        when(responseMapper.toResponse(product)).thenReturn(response);

        String requestJson = String.format("""
                {
                    "categoryId": "%s",
                    "name": "Torta Tres Leches",
                    "slug": "tres-leches",
                    "description": "Deliciosa torta"
                }
                """, categoryId);

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/products", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Torta Tres Leches"))
                .andExpect(jsonPath("$.slug").value("tres-leches"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/products/{productId} - 200 OK")
    void shouldGetProductById() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Alfajor", "alfajor", "Clásico", null);
        ProductResponse response = new ProductResponse(
                product.getId(),
                storeId,
                categoryId,
                "Alfajor",
                "alfajor",
                "Clásico",
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(getProductsService.execute(product.getId(), storeId)).thenReturn(product);
        when(responseMapper.toResponse(product)).thenReturn(response);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products/{productId}", storeId, product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alfajor"));
    }
}