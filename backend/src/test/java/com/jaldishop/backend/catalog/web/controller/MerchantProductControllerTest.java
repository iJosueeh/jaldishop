package com.jaldishop.backend.catalog.web.controller;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.application.command.CreateProductCommand;
import com.jaldishop.backend.catalog.application.service.CreateProductService;
import com.jaldishop.backend.catalog.application.service.GetProductsService;
import com.jaldishop.backend.catalog.application.service.UpdateProductService;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductVariantRepository;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.mapper.ProductResponseMapper;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

    @Mock
    private ProductVariantRepository variantRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MerchantProductController controller = new MerchantProductController(
                createProductService,
                updateProductService,
                getProductsService,
                storeContextService,
                responseMapper,
                variantRepository
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

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products - 403 Forbidden cuando el storeId no pertenece al comerciante")
    void shouldReturn403WhenStoreNotOwnedOnCreate() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."))
                .when(storeContextService).validateStoreOwnership(eq(storeId), any());

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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/products - 403 Forbidden cuando el storeId no pertenece al comerciante")
    void shouldReturn403WhenStoreNotOwnedOnGet() throws Exception {
        UUID storeId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."))
                .when(storeContextService).validateStoreOwnership(eq(storeId), any());

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products", storeId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products - 409 Conflict cuando el slug ya existe")
    void shouldReturn409WhenProductSlugAlreadyExists() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(createProductService.execute(any(CreateProductCommand.class)))
                .thenThrow(new ConflictException("PRODUCT_SLUG_ALREADY_EXISTS", "Ya existe un producto con el slug 'tres-leches' en esta tienda"));

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_SLUG_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Ya existe un producto con el slug 'tres-leches' en esta tienda"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products - 404 Not Found cuando la categoría no existe o no pertenece a la tienda")
    void shouldReturn404WhenCategoryNotFoundOnCreate() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(createProductService.execute(any(CreateProductCommand.class)))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada o no pertenece a la tienda"));

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
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada o no pertenece a la tienda"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/products/{productId} - 404 Not Found cuando el producto no existe")
    void shouldReturn404WhenProductNotFound() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(getProductsService.execute(productId, storeId))
                .thenThrow(new ResourceNotFoundException("Producto no encontrado"));

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products/{productId}", storeId, productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Producto no encontrado"));
    }
}