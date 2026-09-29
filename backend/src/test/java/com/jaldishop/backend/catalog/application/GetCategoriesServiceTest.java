package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCategoriesServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private GetCategoriesService getCategoriesService;

    private UUID storeId;
    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        category = Category.create(storeId, "Postres", "Desc");
    }

    @Test
    @DisplayName("Should list categories by store")
    void shouldListCategoriesByStore() {
        when(categoryRepository.findByStoreId(storeId)).thenReturn(List.of(category));

        List<Category> results = getCategoriesService.execute(storeId);

        assertEquals(1, results.size());
        assertEquals("Postres", results.get(0).getName());
    }

    @Test
    @DisplayName("Should get category by id and store")
    void shouldGetCategoryByIdAndStore() {
        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.of(category));

        Category result = getCategoriesService.execute(categoryId, storeId);

        assertNotNull(result);
        assertEquals("Postres", result.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void shouldThrowWhenNotFound() {
        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> getCategoriesService.execute(categoryId, storeId));
    }
}
