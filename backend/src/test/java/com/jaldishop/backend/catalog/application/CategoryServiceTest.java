package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should successfully create a category when name is unique in store")
    void shouldCreateCategorySuccessfully() {
        var command = new CreateCategoryCommand(storeId, "Postres", "Postres variados");

        when(categoryRepository.existsByStoreIdAndNameIgnoreCase(storeId, "Postres")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.createCategory(command);

        assertNotNull(result);
        assertEquals("Postres", result.getName());
        assertEquals(storeId, result.getStoreId());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should reject category creation if name already exists in the same store")
    void shouldThrowExceptionWhenCategoryNameExistsInStore() {
        var command = new CreateCategoryCommand(storeId, "Postres", "Duplicado");

        when(categoryRepository.existsByStoreIdAndNameIgnoreCase(storeId, "Postres")).thenReturn(true);

        var exception = assertThrows(IllegalArgumentException.class, () ->
                categoryService.createCategory(command)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(categoryRepository, never()).save(any());
    }
}