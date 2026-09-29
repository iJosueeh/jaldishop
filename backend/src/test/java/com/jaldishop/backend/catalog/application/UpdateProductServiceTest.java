package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
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
class UpdateProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private UpdateProductService updateProductService;

    private UUID storeId;
    private UUID categoryId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = Product.create(storeId, categoryId, "Alfajor", "alfajor", "Clasico", null);
    }

    @Test
    @DisplayName("Should update product successfully")
    void shouldUpdateProductSuccessfully() {
        var command = new UpdateProductCommand(productId, storeId, categoryId, "Alfajor de Maicena", "alfajor-maicena", "Nuevo", null, "ACTIVE");
        Category mockCategory = Category.create(storeId, "Dulces", "Desc");

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndStoreId(categoryId, storeId)).thenReturn(Optional.of(mockCategory));
        when(productRepository.existsByStoreIdAndSlugAndIdNot(storeId, "alfajor-maicena", productId)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product updated = updateProductService.execute(command);

        assertNotNull(updated);
        assertEquals("Alfajor de Maicena", updated.getName());
        assertEquals("alfajor-maicena", updated.getSlug());
        assertEquals(ProductStatus.ACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void shouldThrowWhenProductNotFound() {
        var command = new UpdateProductCommand(productId, storeId, categoryId, "Alfajor", "alfajor", null, null, null);

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> updateProductService.execute(command));
    }
}
