package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
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
class UpdateProductVariantServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private UpdateProductVariantService updateVariantService;

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
    @DisplayName("Should update variant successfully")
    void shouldUpdateVariantSuccessfully() {
        var command = new UpdateProductVariantCommand(
                variantId,
                storeId,
                productId,
                "Porcion Mediana",
                "SKU-MED",
                new BigDecimal("25.00"),
                "PEN",
                true,
                "ACTIVE",
                Collections.emptyList()
        );

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByStoreIdAndSkuAndIdNot(storeId, "SKU-MED", variantId)).thenReturn(false);
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductVariant updated = updateVariantService.execute(command);

        assertNotNull(updated);
        assertEquals("Porcion Mediana", updated.getPresentationName());
        assertEquals("SKU-MED", updated.getSku());
        assertEquals(VariantStatus.ACTIVE, updated.getStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when variant not found")
    void shouldThrowWhenVariantNotFound() {
        var command = new UpdateProductVariantCommand(
                variantId,
                storeId,
                productId,
                "Porcion",
                "SKU",
                new BigDecimal("10.00"),
                "PEN",
                false,
                null,
                Collections.emptyList()
        );

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> updateVariantService.execute(command));
    }

    @Test
    @DisplayName("Should throw ConflictException when duplicate SKU exists in the same store")
    void shouldThrowWhenSkuExists() {
        var command = new UpdateProductVariantCommand(
                variantId,
                storeId,
                productId,
                "Porcion",
                "SKU-DUP",
                new BigDecimal("10.00"),
                "PEN",
                false,
                null,
                Collections.emptyList()
        );

        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByStoreIdAndSkuAndIdNot(storeId, "SKU-DUP", variantId)).thenReturn(true);

        assertThrows(ConflictException.class, () -> updateVariantService.execute(command));
    }
}
