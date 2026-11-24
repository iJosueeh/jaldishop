package com.jaldishop.backend.catalog.application.service;

import com.jaldishop.backend.catalog.domain.enums.CategoryStatus;
import com.jaldishop.backend.catalog.domain.model.Category;
import com.jaldishop.backend.catalog.domain.repository.CategoryRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageCategoryLifecycleServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private ManageCategoryLifecycleService service;
    private UUID storeId;
    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        service = new ManageCategoryLifecycleService(categoryRepository);
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        category = new Category(categoryId, storeId, "Bebidas", "Desc", CategoryStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Bloquea hard delete de categoría con 409 CATEGORY_HAS_PRODUCTS si tiene productos")
    void blockDeleteWhenCategoryHasProducts() {
        when(categoryRepository.lockByIdAndStoreIdForUpdate(categoryId, storeId)).thenReturn(Optional.of(category));
        when(categoryRepository.hasProducts(categoryId)).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.delete(categoryId, storeId));

        assertEquals("CATEGORY_HAS_PRODUCTS", ex.getCode());
        verify(categoryRepository, never()).deleteByIdAndStoreId(any(), any());
    }

    @Test
    @DisplayName("Elimina categoría cuando no tiene productos asociados")
    void deleteCategoryWhenEmpty() {
        when(categoryRepository.lockByIdAndStoreIdForUpdate(categoryId, storeId)).thenReturn(Optional.of(category));
        when(categoryRepository.hasProducts(categoryId)).thenReturn(false);

        service.delete(categoryId, storeId);

        verify(categoryRepository).deleteByIdAndStoreId(categoryId, storeId);
    }
}
