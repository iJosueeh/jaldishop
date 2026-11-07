package com.jaldishop.backend.catalog.application.service;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.catalog.domain.repository.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.enums.VariantStatus;
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
    void rejectsVariantFromAnotherStoreWithoutPersistingChanges() {
        UUID foreignStoreId = UUID.randomUUID();
        Product foreignProduct = Product.create(foreignStoreId, UUID.randomUUID(), "Ajeno", "ajeno", null, null);
        ProductVariant foreignVariant = ProductVariant.create(foreignProduct.getId(), "Original", "FOREIGN",
                new BigDecimal("15.00"), "PEN", true, Collections.emptyList());
        var command = new UpdateProductVariantCommand(
                foreignVariant.getId(), storeId, foreignProduct.getId(), "Cambio no autorizado", "CHANGED",
                new BigDecimal("25.00"), "PEN", false, "INACTIVE", Collections.emptyList());
        when(productRepository.findByIdAndStoreId(foreignProduct.getId(), storeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> updateVariantService.execute(command));

        verify(productRepository).findByIdAndStoreId(foreignProduct.getId(), storeId);
        verifyNoInteractions(variantRepository);
        assertEquals("Original", foreignVariant.getPresentationName());
        assertEquals("FOREIGN", foreignVariant.getSku());
        assertEquals(new BigDecimal("15.00"), foreignVariant.getPriceAmount());
        assertTrue(foreignVariant.isTracksInventory());
        assertEquals(VariantStatus.ACTIVE, foreignVariant.getStatus());
    }

    @Test
    void rejectsVariantFromAnotherProductWithoutPersistingChanges() {
        Product otherProduct = Product.create(storeId, UUID.randomUUID(), "Otro", "otro", null, null);
        ProductVariant otherVariant = ProductVariant.create(otherProduct.getId(), "Original", "OTHER",
                new BigDecimal("15.00"), "PEN", true, Collections.emptyList());
        var command = new UpdateProductVariantCommand(
                otherVariant.getId(), storeId, productId, "Cambio no autorizado", "CHANGED",
                new BigDecimal("25.00"), "PEN", false, "INACTIVE", Collections.emptyList());
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.findById(otherVariant.getId())).thenReturn(Optional.of(otherVariant));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> updateVariantService.execute(command));

        assertEquals("La variante no pertenece al producto especificado", exception.getMessage());
        verify(variantRepository).findById(otherVariant.getId());
        verify(variantRepository, never()).save(any(ProductVariant.class));
        verifyNoMoreInteractions(variantRepository);
        assertEquals("Original", otherVariant.getPresentationName());
        assertEquals("OTHER", otherVariant.getSku());
        assertEquals(new BigDecimal("15.00"), otherVariant.getPriceAmount());
        assertTrue(otherVariant.isTracksInventory());
        assertEquals(VariantStatus.ACTIVE, otherVariant.getStatus());
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
