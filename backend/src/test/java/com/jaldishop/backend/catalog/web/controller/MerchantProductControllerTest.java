package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductCommand;
import com.jaldishop.backend.catalog.application.ProductService;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantProductControllerTest {

    @Mock
    private ProductService productService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MerchantProductController controller = new MerchantProductController(productService);

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

        when(productService.createProduct(any(CreateProductCommand.class))).thenReturn(product);

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

        when(productService.getProductByIdAndStore(product.getId(), storeId)).thenReturn(product);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products/{productId}", storeId, product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alfajor"));
    }
}