package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.GetStoreCategoriesService;
import com.jaldishop.backend.store.domain.StoreCategory;
import com.jaldishop.backend.store.domain.StoreCategoryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoreCategoryControllerTest {

    @Mock
    private GetStoreCategoriesService getStoreCategoriesService;

    @InjectMocks
    private StoreCategoryController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/store-categories - Retorna lista de categorias de tiendas (200 OK)")
    void getStoreCategoriesSuccess() throws Exception {
        UUID id = UUID.randomUUID();
        StoreCategory cat = new StoreCategory(
                id, "Tecnologia", "tecnologia", "Gadgets y computo",
                StoreCategoryStatus.ACTIVE, Instant.now(), Instant.now()
        );

        when(getStoreCategoriesService.execute()).thenReturn(List.of(cat));

        mockMvc.perform(get("/api/v1/store-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Tecnologia"))
                .andExpect(jsonPath("$[0].slug").value("tecnologia"))
                .andExpect(jsonPath("$[0].description").value("Gadgets y computo"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }
}
