package com.jaldishop.backend.catalog.web.mapper;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.Category;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CategoryResponseMapperTest {

    private final CategoryResponseMapper mapper = new CategoryResponseMapper();

    @Test
    @DisplayName("Should map category domain entity to CategoryResponse DTO")
    void shouldMapCategoryToResponse() {
        UUID storeId = UUID.randomUUID();
        Category category = Category.create(storeId, "Postres", "Postres artesanales");

        CategoryResponse response = mapper.toResponse(category);

        assertNotNull(response);
        assertEquals(category.getId(), response.id());
        assertEquals(storeId, response.storeId());
        assertEquals("Postres", response.name());
        assertEquals("Postres artesanales", response.description());
        assertEquals("ACTIVE", response.status());
    }

    @Test
    @DisplayName("Should return null when category is null")
    void shouldReturnNullWhenCategoryIsNull() {
        assertNull(mapper.toResponse(null));
    }
}
