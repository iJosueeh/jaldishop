package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductVariantJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.mapper.InventoryResponseMapper;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryConcurrencyUnitTest {

    @Mock
    private ProductVariantJpaRepository productVariantJpaRepository;
    @Mock
    private InventoryJpaRepository inventoryJpaRepository;

    private UpdateInventoryStockService updateInventoryStockService;
    private final UUID storeId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        InventoryResponseMapper responseMapper = new InventoryResponseMapper();
        GetVariantInventoryService getVariantInventoryService = new GetVariantInventoryService(inventoryJpaRepository, responseMapper);
        updateInventoryStockService = new UpdateInventoryStockService(productVariantJpaRepository, inventoryJpaRepository, getVariantInventoryService);
    }

    @Test
    @DisplayName("Simulación de concurrencia: múltiples hilos intentando inicializar/ajustar stock")
    void testConcurrentStockInitialization() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "Presentación 500g", "SKU-500",
                BigDecimal.valueOf(25), "PEN", true, "ACTIVE", Instant.now(), Instant.now()
        );

        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));
        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(variantId), any()))
                .thenReturn(0);
        when(inventoryJpaRepository.findVariantInventoryByStoreIdAndVariantId(storeId, variantId))
                .thenReturn(Optional.of(new com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection() {
                    @Override public UUID getVariantId() { return variantId; }
                    @Override public UUID getProductId() { return productId; }
                    @Override public String getProductName() { return "Café"; }
                    @Override public String getPresentationName() { return "500g"; }
                    @Override public String getSku() { return "SKU-500"; }
                    @Override public boolean isTracksInventory() { return true; }
                    @Override public String getStatus() { return "ACTIVE"; }
                    @Override public Integer getPhysicalQuantity() { return 50; }
                    @Override public Integer getLowStockThreshold() { return 10; }
                    @Override public Instant getInventoryUpdatedAt() { return Instant.now(); }
                }));

        for (int i = 0; i < threadCount; i++) {
            final int quantity = 50 + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    updateInventoryStockService.execute(storeId, variantId, quantity);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(errorCount.get()).isEqualTo(0);
        verify(inventoryJpaRepository, times(threadCount)).upsertStock(eq(variantId), anyInt(), any());
    }

    @Test
    @DisplayName("Ajuste concurrente con hold activo: falla si cantidad < hold activo")
    void testConcurrentStockAdjustmentFailsIfBelowActiveHold() {
        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "Presentación 500g", "SKU-500",
                BigDecimal.valueOf(25), "PEN", true, "ACTIVE", Instant.now(), Instant.now()
        );

        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));
        // Simulamos un hold activo en checkout de 10 unidades
        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(variantId), any()))
                .thenReturn(10);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                updateInventoryStockService.execute(storeId, variantId, 9)
        ).isInstanceOf(ConflictException.class)
         .hasMessageContaining("existen 10 unidades reservadas activas");

        verify(inventoryJpaRepository, never()).upsertStock(any(), anyInt(), any());
    }
}
