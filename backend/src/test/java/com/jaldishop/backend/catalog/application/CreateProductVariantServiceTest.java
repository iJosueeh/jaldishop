package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateProductVariantServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CreateProductVariantService createVariantService;

    private UUID storeId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = Product.create(storeId, UUID.randomUUID(), "Torta", "torta", null, null);
    }

    @Test
    @DisplayName("Should create variant successfully")
    void shouldCreateVariantSuccessfully() {
        var command = new CreateProductVariantCommand(
                storeId,
                productId,
                "Porcion Individual",
                "TORTA-IND-01",
                new BigDecimal("15.00"),
                "PEN",
                true,
                Collections.emptyList()
        );

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("TORTA-IND-01")).thenReturn(false);
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariant result = createVariantService.execute(command);

        assertNotNull(result);
        assertEquals("Porcion Individual", result.getPresentationName());
        assertEquals("TORTA-IND-01", result.getSku());
        verify(variantRepository).save(any(ProductVariant.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product does not belong to store")
    void shouldThrowWhenProductNotFound() {
        var command = new CreateProductVariantCommand(
                storeId,
                productId,
                "Porcion",
                "SKU-1",
                new BigDecimal("10.00"),
                "PEN",
                false,
                Collections.emptyList()
        );

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> createVariantService.execute(command));
    }

    @Test
    @DisplayName("Should throw ConflictException when SKU already exists")
    void shouldThrowWhenSkuExists() {
        var command = new CreateProductVariantCommand(
                storeId,
                productId,
                "Porcion",
                "SKU-EXISTS",
                new BigDecimal("10.00"),
                "PEN",
                false,
                Collections.emptyList()
        );

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySku("SKU-EXISTS")).thenReturn(true);

        assertThrows(ConflictException.class, () -> createVariantService.execute(command));
    }
}
