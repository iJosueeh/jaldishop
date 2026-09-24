package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateProductCommand;
import com.jaldishop.backend.catalog.application.ProductService;
import com.jaldishop.backend.catalog.domain.Product;
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

import java.util.Collections;
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

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private JwtPrincipal mockPrincipal;

    private MockMvc mockMvc;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        MerchantProductController controller = new MerchantProductController(productService, storeContextService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        auth = new UsernamePasswordAuthenticationToken(mockPrincipal, null, Collections.emptyList());
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products - 201 Created cuando es válido")
    void shouldCreateProduct() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Torta Tres Leches", "tres-leches", "Deliciosa torta", null);

        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
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
                        .principal(auth)
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

        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(productService.getProductByIdAndStore(product.getId(), storeId)).thenReturn(product);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products/{productId}", storeId, product.getId())
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alfajor"));
    }
}