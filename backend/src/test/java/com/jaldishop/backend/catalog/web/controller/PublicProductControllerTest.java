package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.GetProductsQuery;
import com.jaldishop.backend.catalog.application.GetProductsService;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.mapper.ProductResponseMapper;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.GetStoreBySlugService;
import com.jaldishop.backend.store.domain.Store;
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

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
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
}
