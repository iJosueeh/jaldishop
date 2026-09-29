package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CreateCategoryCommand;
import com.jaldishop.backend.catalog.application.CreateCategoryService;
import com.jaldishop.backend.catalog.application.GetCategoriesService;
import com.jaldishop.backend.catalog.application.UpdateCategoryService;
import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import com.jaldishop.backend.catalog.web.mapper.CategoryResponseMapper;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantCategoryControllerTest {

    @Mock
    private CreateCategoryService createCategoryService;

    @Mock
    private UpdateCategoryService updateCategoryService;

    @Mock
    private GetCategoriesService getCategoriesService;

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private CategoryResponseMapper responseMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MerchantCategoryController controller = new MerchantCategoryController(
                createCategoryService,
                updateCategoryService,
                getCategoriesService,
                storeContextService,
                responseMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/categories - 201 Created cuando es válido")
    void shouldCreateCategory() throws Exception {
        UUID storeId = UUID.randomUUID();
        Category category = Category.create(storeId, "Postres", "Postres artesanales");
        CategoryResponse response = new CategoryResponse(
                category.getId(),
                storeId,
                "Postres",
                "Postres artesanales",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(createCategoryService.execute(any(CreateCategoryCommand.class))).thenReturn(category);
        when(responseMapper.toResponse(category)).thenReturn(response);

        String requestJson = """
                {
                    "name": "Postres",
                    "description": "Postres artesanales"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/categories", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Postres"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/categories - 200 OK con lista de categorías")
    void shouldGetCategories() throws Exception {
        UUID storeId = UUID.randomUUID();
        Category category = Category.create(storeId, "Bebidas", null);
        CategoryResponse response = new CategoryResponse(
                category.getId(),
                storeId,
                "Bebidas",
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );

        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(getCategoriesService.execute(storeId)).thenReturn(List.of(category));
        when(responseMapper.toResponse(category)).thenReturn(response);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/categories", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bebidas"));
    }
}