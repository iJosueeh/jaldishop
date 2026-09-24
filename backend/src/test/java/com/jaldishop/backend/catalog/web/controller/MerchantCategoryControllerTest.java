package com.jaldishop.backend.catalog.web.controller;

import com.jaldishop.backend.catalog.application.CategoryService;
import com.jaldishop.backend.catalog.application.CreateCategoryCommand;
import com.jaldishop.backend.catalog.domain.Category;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantCategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private JwtPrincipal mockPrincipal;

    private MockMvc mockMvc;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        MerchantCategoryController controller = new MerchantCategoryController(categoryService, storeContextService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        auth = new UsernamePasswordAuthenticationToken(mockPrincipal, null, Collections.emptyList());
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/categories - 201 Created cuando es válido")
    void shouldCreateCategory() throws Exception {
        UUID storeId = UUID.randomUUID();
        Category category = Category.create(storeId, "Postres", "Postres artesanales");

        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(categoryService.createCategory(any(CreateCategoryCommand.class))).thenReturn(category);

        String requestJson = """
                {
                    "name": "Postres",
                    "description": "Postres artesanales"
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/categories", storeId)
                        .principal(auth)
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

        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(categoryService.getCategoriesByStore(storeId)).thenReturn(List.of(category));

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/categories", storeId)
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bebidas"));
    }
}