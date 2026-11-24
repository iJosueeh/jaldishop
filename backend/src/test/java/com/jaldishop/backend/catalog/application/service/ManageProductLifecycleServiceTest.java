package com.jaldishop.backend.catalog.application.service;

import com.jaldishop.backend.catalog.domain.enums.ProductStatus;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.port.CatalogMediaCleaner;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManageProductLifecycleServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CatalogMediaCleaner mediaCleaner;

    private ManageProductLifecycleService service;
    private UUID storeId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new ManageProductLifecycleService(productRepository, mediaCleaner);
        storeId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = new Product(productId, storeId, UUID.randomUUID(), "Pollo", "pollo", "Desc", "https://res.cloudinary.com/demo/image/upload/v1/jaldishop/p1.jpg", ProductStatus.ACTIVE, Instant.now(), Instant.now());
    }

    @Test
    @DisplayName("Bloquea hard delete de producto con 409 PRODUCT_HAS_OPERATIONAL_HISTORY si tiene historial")
    void blockDeleteWhenHasOperationalHistory() {
        when(productRepository.lockByIdAndStoreIdForUpdate(productId, storeId)).thenReturn(Optional.of(product));
        when(productRepository.hasOperationalHistory(productId)).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.delete(productId, storeId));

        assertEquals("PRODUCT_HAS_OPERATIONAL_HISTORY", ex.getCode());
        assertEquals("DEACTIVATE", ex.getMetadata().get("suggestedAction"));
        assertEquals("ACTIVE", ex.getMetadata().get("currentStatus"));
        verify(productRepository, never()).deleteProductAndSafeOrphans(any(), any());
        verify(mediaCleaner, never()).enqueueAssetDeletions(any());
    }

    @Test
    @DisplayName("Elimina producto y encola imágenes en outbox cuando no tiene historial")
    void deleteProductAndEnqueueImagesWhenClean() {
        when(productRepository.lockByIdAndStoreIdForUpdate(productId, storeId)).thenReturn(Optional.of(product));
        when(productRepository.hasOperationalHistory(productId)).thenReturn(false);
        when(productRepository.findProductImageUrls(productId)).thenReturn(List.of("https://res.cloudinary.com/demo/image/upload/v1/jaldishop/p1.jpg"));

        service.delete(productId, storeId);

        verify(productRepository).deleteProductAndSafeOrphans(productId, storeId);
        verify(mediaCleaner).enqueueAssetDeletions(List.of("https://res.cloudinary.com/demo/image/upload/v1/jaldishop/p1.jpg"));
    }

    @Test
    @DisplayName("Actualiza estado del producto a INACTIVE vía PATCH")
    void updateStatusToInactive() {
        when(productRepository.findByIdAndStoreId(productId, storeId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product updated = service.updateStatus(productId, storeId, "INACTIVE");

        assertEquals(ProductStatus.INACTIVE, updated.getStatus());
    }
}
