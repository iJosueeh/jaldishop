package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.CategoryStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private UpdateCategoryService updateCategoryService;

    private UUID storeId;
    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        category = Category.create(storeId, "Postres", "Postres variados");
    }

    @Test
    @DisplayName("Should update category successfully")
    void shouldUpdateCategorySuccessfully() {
        var command = new UpdateCategoryCommand(categoryId, storeId, "Postres Finos", "Nuevos", "ACTIVE");

        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByStoreIdAndNameIgnoreCaseAndIdNot(storeId, "Postres Finos", categoryId)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category updated = updateCategoryService.execute(command);

        assertNotNull(updated);
        assertEquals("Postres Finos", updated.getName());
        assertEquals("Nuevos", updated.getDescription());
        assertEquals(CategoryStatus.ACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when category not found")
    void shouldThrowWhenCategoryNotFound() {
        var command = new UpdateCategoryCommand(categoryId, storeId, "Postres", null, null);

        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> updateCategoryService.execute(command));
    }

    @Test
    @DisplayName("Should throw ConflictException when duplicate name exists")
    void shouldThrowWhenDuplicateNameExists() {
        var command = new UpdateCategoryCommand(categoryId, storeId, "Bebidas", null, null);

        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByStoreIdAndNameIgnoreCaseAndIdNot(storeId, "Bebidas", categoryId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> updateCategoryService.execute(command));
    }
}
