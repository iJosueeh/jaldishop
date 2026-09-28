package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
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
class GetProductsServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductsService getProductsService;

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
    @DisplayName("Should get products by store")
    void shouldGetProductsByStore() {
        when(productRepository.findByStoreId(storeId)).thenReturn(List.of(product));

        List<Product> results = getProductsService.execute(new GetProductsQuery(storeId, null));

        assertEquals(1, results.size());
        assertEquals("Alfajor", results.get(0).getName());
    }

    @Test
    @DisplayName("Should get products by store and category")
    void shouldGetProductsByStoreAndCategory() {
        when(productRepository.findByStoreIdAndCategoryId(storeId, categoryId)).thenReturn(List.of(product));

        List<Product> results = getProductsService.execute(new GetProductsQuery(storeId, categoryId));

        assertEquals(1, results.size());
        assertEquals("Alfajor", results.get(0).getName());
    }

    @Test
    @DisplayName("Should get product by id and store")
    void shouldGetProductByIdAndStore() {
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));

        Product result = getProductsService.execute(productId, storeId);

        assertNotNull(result);
        assertEquals("Alfajor", result.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void shouldThrowWhenNotFound() {
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> getProductsService.execute(productId, storeId));
    }
}
