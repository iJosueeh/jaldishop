package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.GetPublicCategoriesService;
import com.jaldishop.backend.catalog.web.mapper.CategoryResponseMapper;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.util.MethodValidationTestSupport;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicCategoryControllerTest {

    @Mock
    private GetPublicCategoriesService service;

    @Mock
    private GetStoreBySlugService getStoreBySlugService;

    @Mock
    private CategoryResponseMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PublicCategoryController controller = new PublicCategoryController(
                service,
                getStoreBySlugService,
                mapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(MethodValidationTestSupport.withParameterValidation(controller))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/stores/{storeId}/categories - 200 OK con lista vacía")
    void shouldReturnCategoriesByStoreId() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(service.execute(storeId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/stores/{storeId}/categories", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/stores/slug/{slug}/categories - 400 VALIDATION_ERROR con slug demasiado largo y recuperación posterior")
    void shouldRejectTooLongSlug() throws Exception {
        mockMvc.perform(get("/api/v1/stores/slug/{slug}/categories", "a".repeat(181)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(service, getStoreBySlugService);

        UUID storeId = UUID.randomUUID();
        Store mockStore = org.mockito.Mockito.mock(Store.class);
        when(mockStore.getId()).thenReturn(storeId);
        when(getStoreBySlugService.execute("mi-tienda")).thenReturn(mockStore);
        when(service.execute(storeId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/stores/slug/{slug}/categories", "mi-tienda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
