package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetProductVariantsServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetProductVariantsService getVariantsService;

    private UUID storeId;
    private UUID productId;
    private UUID variantId;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        product = Product.create(storeId, UUID.randomUUID(), "Torta", "torta", null, null);
        variant = ProductVariant.create(productId, "Porcion Individual", "SKU-IND", new BigDecimal("15.00"), "PEN", true, Collections.emptyList());
    }

    @Test
    @DisplayName("Should list variants by product")
    void shouldListVariantsByProduct() {
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductId(productId)).thenReturn(List.of(variant));

        List<ProductVariant> results = getVariantsService.execute(productId, storeId);

        assertEquals(1, results.size());
        assertEquals("Porcion Individual", results.get(0).getPresentationName());
    }

    @Test
    @DisplayName("Should get variant by id")
    void shouldGetVariantById() {
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        ProductVariant result = getVariantsService.execute(variantId, productId, storeId);

        assertNotNull(result);
        assertEquals("Porcion Individual", result.getPresentationName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> getVariantsService.execute(productId, storeId));
    }
}
