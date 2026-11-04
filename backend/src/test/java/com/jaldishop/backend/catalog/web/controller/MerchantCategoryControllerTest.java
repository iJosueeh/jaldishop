package com.jaldishop.backend.catalog.web.controller;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.application.command.CreateCategoryCommand;
import com.jaldishop.backend.catalog.application.service.CreateCategoryService;
import com.jaldishop.backend.catalog.application.service.GetCategoriesService;
import com.jaldishop.backend.catalog.application.service.UpdateCategoryService;
import com.jaldishop.backend.catalog.domain.model.Category;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import com.jaldishop.backend.catalog.web.mapper.CategoryResponseMapper;
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
import java.util.List;
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

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/categories - 403 Forbidden cuando el storeId no pertenece al comerciante")
    void shouldReturn403WhenStoreNotOwnedOnCreate() throws Exception {
        UUID storeId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."))
                .when(storeContextService).validateStoreOwnership(eq(storeId), any());

        String requestJson = """
                {
                    "name": "Postres",
                    "description": "Postres artesanales"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/categories", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/categories - 403 Forbidden cuando el storeId no pertenece al comerciante")
    void shouldReturn403WhenStoreNotOwnedOnGet() throws Exception {
        UUID storeId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."))
                .when(storeContextService).validateStoreOwnership(eq(storeId), any());

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/categories", storeId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/categories - 409 Conflict cuando ya existe una categoría con ese nombre")
    void shouldReturn409WhenCategoryAlreadyExists() throws Exception {
        UUID storeId = UUID.randomUUID();
        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(createCategoryService.execute(any(CreateCategoryCommand.class)))
                .thenThrow(new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría con este nombre en la tienda"));

        String requestJson = """
                {
                    "name": "Postres",
                    "description": "Postres artesanales"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/categories", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Ya existe una categoría con este nombre en la tienda"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/categories/{categoryId} - 404 Not Found cuando no existe la categoría")
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(getCategoriesService.execute(categoryId, storeId))
                .thenThrow(new ResourceNotFoundException("Categoría no encontrada"));

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/categories/{categoryId}", storeId, categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Categoría no encontrada"));
    }
}