package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private UUID storeId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should create product with auto-generated slug when slug is not provided")
    void shouldCreateProductWithAutoSlug() {
        var command = new CreateProductCommand(storeId, categoryId, "Cheesecake de Fresa", null, "Rico cheesecake", null);
        Category mockCategory = Category.create(storeId, "Tortas", "Desc");

        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.of(mockCategory));
        when(productRepository.existsByStoreIdAndSlug(storeId, "cheesecake-de-fresa")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product result = productService.createProduct(command);

        assertNotNull(result);
        assertEquals("cheesecake-de-fresa", result.getSlug());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should fail when category does not belong to the store")
    void shouldThrowWhenCategoryDoesNotBelongToStore() {
        var command = new CreateProductCommand(storeId, categoryId, "Galletas", null, null, null);

        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                productService.createProduct(command)
        );
        verify(productRepository, never()).save(any());
    }
}