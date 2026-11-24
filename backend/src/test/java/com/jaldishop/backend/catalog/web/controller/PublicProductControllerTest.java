package com.jaldishop.backend.catalog.web.controller;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.application.query.GetProductsQuery;
import com.jaldishop.backend.catalog.application.service.GetProductsService;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.enums.VariantStatus;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.mapper.ProductResponseMapper;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.util.MethodValidationTestSupport;
import com.jaldishop.backend.store.application.service.GetStoreBySlugService;
import com.jaldishop.backend.store.domain.model.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicProductControllerTest {

    @Mock
    private GetProductsService getProductsService;

    @Mock
    private ProductResponseMapper responseMapper;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private GetStoreBySlugService getStoreBySlugService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PublicProductController controller = new PublicProductController(
                getProductsService,
                responseMapper,
                variantRepository,
                getStoreBySlugService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(MethodValidationTestSupport.withParameterValidation(controller))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/stores/{storeId}/products - 200 OK")
    void shouldGetActiveProductsByStoreId() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Croissant", "croissant", "De mantequilla", null);

        ProductResponse response = new ProductResponse(
                product.getId(),
                storeId,
                categoryId,
                "Croissant",
                "croissant",
                "De mantequilla",
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        when(getProductsService.execute(any(GetProductsQuery.class))).thenReturn(List.of(product));
        when(variantRepository.findByStoreId(storeId)).thenReturn(List.of());
        when(responseMapper.toResponse(product)).thenReturn(response);

        mockMvc.perform(get("/api/v1/stores/{storeId}/products", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Croissant"));
    }

    @Test
    @DisplayName("GET /api/v1/stores/slug/{slug}/products - 200 OK")
    void shouldGetActiveProductsBySlug() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Pastel", "pastel", "De lúcuma", null);

        Store mockStore = org.mockito.Mockito.mock(Store.class);
        when(mockStore.getId()).thenReturn(storeId);
        when(getStoreBySlugService.execute("panaderia-pepe")).thenReturn(mockStore);

        ProductResponse response = new ProductResponse(
                product.getId(),
                storeId,
                categoryId,
                "Pastel",
                "pastel",
                "De lúcuma",
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        when(getProductsService.execute(any(GetProductsQuery.class))).thenReturn(List.of(product));
        when(variantRepository.findByStoreId(storeId)).thenReturn(List.of());
        when(responseMapper.toResponse(product)).thenReturn(response);

        mockMvc.perform(get("/api/v1/stores/slug/{slug}/products", "panaderia-pepe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pastel"));
    }

    @Test
    @DisplayName("GET /api/v1/stores/slug/{storeSlug}/products/{productSlug} retorna solo variantes activas")
    void shouldGetActiveProductByStoreAndProductSlug() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Product product = Product.create(storeId, categoryId, "Pastel", "pastel", "De lúcuma", null);
        ProductVariant activeVariant = ProductVariant.create(
                product.getId(), "Unidad", "PASTEL-001", new java.math.BigDecimal("12.00"), "PEN", true, List.of()
        );
        ProductVariant inactiveVariant = new ProductVariant(
                UUID.randomUUID(), product.getId(), "Caja", "PASTEL-002", new java.math.BigDecimal("20.00"),
                "PEN", true, VariantStatus.INACTIVE, List.of(), Instant.now(), Instant.now()
        );
        Store store = org.mockito.Mockito.mock(Store.class);
        ProductResponse response = new ProductResponse(
                product.getId(), storeId, categoryId, "Pastel", "pastel", "De lúcuma", null,
                "ACTIVE", Instant.now(), Instant.now()
        );

        when(store.getId()).thenReturn(storeId);
        when(getStoreBySlugService.execute("panaderia-pepe")).thenReturn(store);
        when(getProductsService.executeBySlug(storeId, "pastel")).thenReturn(product);
        when(variantRepository.findByProductId(product.getId())).thenReturn(List.of(activeVariant, inactiveVariant));
        when(responseMapper.toResponse(product, List.of(activeVariant))).thenReturn(response);

        mockMvc.perform(get("/api/v1/stores/slug/{storeSlug}/products/{productSlug}", "panaderia-pepe", "pastel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("pastel"));

        verify(responseMapper).toResponse(product, List.of(activeVariant));
    }

    @Test
    @DisplayName("GET /api/v1/stores/slug/{slug}/products - 400 VALIDATION_ERROR con slug demasiado largo y recuperación posterior")
    void shouldRejectTooLongSlug() throws Exception {
        mockMvc.perform(get("/api/v1/stores/slug/{slug}/products", "a".repeat(181)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(getStoreBySlugService, getProductsService, variantRepository);

        UUID storeId = UUID.randomUUID();
        Store mockStore = org.mockito.Mockito.mock(Store.class);
        when(mockStore.getId()).thenReturn(storeId);
        when(getStoreBySlugService.execute("panaderia-pepe")).thenReturn(mockStore);
        when(getProductsService.execute(any(GetProductsQuery.class))).thenReturn(List.of());
        when(variantRepository.findByStoreId(storeId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/stores/slug/{slug}/products", "panaderia-pepe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
