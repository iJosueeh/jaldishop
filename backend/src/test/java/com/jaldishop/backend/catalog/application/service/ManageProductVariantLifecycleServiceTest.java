package com.jaldishop.backend.catalog.application.service;

import com.jaldishop.backend.catalog.domain.enums.ProductStatus;
import com.jaldishop.backend.catalog.domain.enums.VariantStatus;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.catalog.domain.repository.ProductVariantRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageProductVariantLifecycleServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    private ManageProductVariantLifecycleService service;
    private UUID storeId;
    private UUID productId;
    private UUID variantId;
    private Product product;
    private ProductVariant variant;

    @BeforeEach
    void setUp() {
        service = new ManageProductVariantLifecycleService(variantRepository, productRepository);
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        variantId = UUID.randomUUID();
        product = new Product(productId, storeId, UUID.randomUUID(), "Pollo", "pollo", "Desc", null, ProductStatus.ACTIVE, Instant.now(), Instant.now());
        variant = new ProductVariant(variantId, productId, "Entero", "SKU-1", BigDecimal.TEN, "PEN", true, VariantStatus.ACTIVE, List.of(), Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Bloquea hard delete de variante con 409 LAST_ACTIVE_VARIANT si es la única activa")
    void blockDeleteWhenLastActiveVariant() {
        when(variantRepository.lockByIdAndStoreIdForUpdate(variantId, storeId)).thenReturn(Optional.of(variant));
        when(variantRepository.hasOperationalHistory(variantId)).thenReturn(false);
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.countActiveByProductIdAndIdNot(productId, variantId)).thenReturn(0L);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.delete(variantId, productId, storeId));

        assertEquals("LAST_ACTIVE_VARIANT", ex.getCode());
        verify(variantRepository, never()).deleteVariantAndSafeOrphans(any(), any());
    }

    @Test
    @DisplayName("Elimina variante si no tiene historial y existe otra variante activa")
    void deleteVariantWhenCleanAndOtherActiveExists() {
        when(variantRepository.lockByIdAndStoreIdForUpdate(variantId, storeId)).thenReturn(Optional.of(variant));
        when(variantRepository.hasOperationalHistory(variantId)).thenReturn(false);
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(variantRepository.countActiveByProductIdAndIdNot(productId, variantId)).thenReturn(1L);

        service.delete(variantId, productId, storeId);

        verify(variantRepository).deleteVariantAndSafeOrphans(variantId, storeId);
    }
}
