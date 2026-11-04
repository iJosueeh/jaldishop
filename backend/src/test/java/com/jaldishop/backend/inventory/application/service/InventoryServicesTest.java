package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductVariantJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.mapper.InventoryResponseMapper;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServicesTest {

    @Mock
    private InventoryJpaRepository inventoryJpaRepository;
    @Mock
    private ProductVariantJpaRepository productVariantJpaRepository;

    private InventoryResponseMapper responseMapper;
    private GetStoreInventoryService getStoreInventoryService;
    private GetVariantInventoryService getVariantInventoryService;
    private GetLowStockInventoryService getLowStockInventoryService;
    private UpdateInventoryStockService updateInventoryStockService;
    private UpdateInventoryThresholdService updateInventoryThresholdService;

    private final UUID storeId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        responseMapper = new InventoryResponseMapper();
        getVariantInventoryService = new GetVariantInventoryService(inventoryJpaRepository, responseMapper);
        getStoreInventoryService = new GetStoreInventoryService(inventoryJpaRepository, responseMapper);
        getLowStockInventoryService = new GetLowStockInventoryService(inventoryJpaRepository, responseMapper);
        updateInventoryStockService = new UpdateInventoryStockService(productVariantJpaRepository, inventoryJpaRepository, getVariantInventoryService);
        updateInventoryThresholdService = new UpdateInventoryThresholdService(productVariantJpaRepository, inventoryJpaRepository, getVariantInventoryService);
    }

    private StoreVariantInventoryProjection createProjection(boolean tracks, Integer physical, Integer threshold) {
        return new StoreVariantInventoryProjection() {
            @Override public UUID getVariantId() { return variantId; }
            @Override public UUID getProductId() { return productId; }
            @Override public String getProductName() { return "Café Geisha"; }
            @Override public String getPresentationName() { return "250g"; }
            @Override public String getSku() { return "GEISHA-250"; }
            @Override public boolean isTracksInventory() { return tracks; }
            @Override public String getStatus() { return "ACTIVE"; }
            @Override public Integer getPhysicalQuantity() { return physical; }
            @Override public Integer getLowStockThreshold() { return threshold; }
            @Override public Instant getInventoryUpdatedAt() { return Instant.now(); }
        };
    }

    @Test
    @DisplayName("GetStoreInventory: mapea correctamente variantes configuradas y no configuradas")
    void testGetStoreInventory() {
        StoreVariantInventoryProjection projConfigured = createProjection(true, 10, 3);
        when(inventoryJpaRepository.findAllVariantInventoriesByStoreId(storeId))
                .thenReturn(List.of(projConfigured));
        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(variantId), any()))
                .thenReturn(2);

        List<InventoryResponse> result = getStoreInventoryService.execute(storeId);

        assertThat(result).hasSize(1);
        InventoryResponse resp = result.get(0);
        assertThat(resp.inventoryStatus()).isEqualTo("CONFIGURED");
        assertThat(resp.physicalQuantity()).isEqualTo(10);
        assertThat(resp.reservedQuantity()).isEqualTo(2);
        assertThat(resp.availableQuantity()).isEqualTo(8);
        assertThat(resp.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("GetVariantInventory: cuando tracksInventory=false retorna NOT_TRACKED y cantidades nulas")
    void testGetVariantNotTracked() {
        StoreVariantInventoryProjection proj = createProjection(false, null, null);
        when(inventoryJpaRepository.findVariantInventoryByStoreIdAndVariantId(storeId, variantId))
                .thenReturn(Optional.of(proj));

        InventoryResponse resp = getVariantInventoryService.execute(storeId, variantId);

        assertThat(resp.inventoryStatus()).isEqualTo("NOT_TRACKED");
        assertThat(resp.physicalQuantity()).isNull();
        assertThat(resp.reservedQuantity()).isNull();
        assertThat(resp.availableQuantity()).isNull();
        assertThat(resp.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("GetVariantInventory: cuando tracksInventory=true y physicalQuantity=null retorna NOT_CONFIGURED")
    void testGetVariantNotConfigured() {
        StoreVariantInventoryProjection proj = createProjection(true, null, null);
        when(inventoryJpaRepository.findVariantInventoryByStoreIdAndVariantId(storeId, variantId))
                .thenReturn(Optional.of(proj));

        InventoryResponse resp = getVariantInventoryService.execute(storeId, variantId);

        assertThat(resp.inventoryStatus()).isEqualTo("NOT_CONFIGURED");
        assertThat(resp.physicalQuantity()).isNull();
        assertThat(resp.isLowStock()).isFalse();
    }

    @Test
    @DisplayName("UpdateInventoryStock: actualiza stock físico absoluto si supera reservas activas")
    void testUpdateInventoryStockSuccess() {
        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "250g", "SKU1",
                BigDecimal.TEN, "PEN", true, "ACTIVE", Instant.now(), Instant.now()
        );
        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));
        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(variantId), any()))
                .thenReturn(3);

        StoreVariantInventoryProjection freshProjection = createProjection(true, 15, 5);
        when(inventoryJpaRepository.findVariantInventoryByStoreIdAndVariantId(storeId, variantId))
                .thenReturn(Optional.of(freshProjection));

        InventoryResponse resp = updateInventoryStockService.execute(storeId, variantId, 15);

        verify(inventoryJpaRepository).upsertStock(eq(variantId), eq(15), any());
        assertThat(resp.physicalQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("UpdateInventoryStock: rechaza si la variante tiene tracksInventory=false")
    void testUpdateInventoryStockTracksFalseThrows() {
        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "250g", "SKU1",
                BigDecimal.TEN, "PEN", false, "ACTIVE", Instant.now(), Instant.now()
        );
        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> updateInventoryStockService.execute(storeId, variantId, 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tracksInventory=false");
    }

    @Test
    @DisplayName("UpdateInventoryStock: rechaza ajuste si nuevo stock < reservas activas vigentes")
    void testUpdateInventoryStockBelowReservedThrows() {
        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "250g", "SKU1",
                BigDecimal.TEN, "PEN", true, "ACTIVE", Instant.now(), Instant.now()
        );
        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));
        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(variantId), any()))
                .thenReturn(8);

        assertThatThrownBy(() -> updateInventoryStockService.execute(storeId, variantId, 5))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("CANNOT_REDUCE_BELOW_RESERVED"));
    }

    @Test
    @DisplayName("UpdateInventoryThreshold: actualiza umbral si inventario ya está configurado")
    void testUpdateThresholdSuccess() {
        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "250g", "SKU1",
                BigDecimal.TEN, "PEN", true, "ACTIVE", Instant.now(), Instant.now()
        );
        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));

        InventoryEntity inventory = new InventoryEntity(variantId, 20, 2, Instant.now());
        when(inventoryJpaRepository.findById(variantId)).thenReturn(Optional.of(inventory));

        StoreVariantInventoryProjection freshProjection = createProjection(true, 20, 6);
        when(inventoryJpaRepository.findVariantInventoryByStoreIdAndVariantId(storeId, variantId))
                .thenReturn(Optional.of(freshProjection));

        InventoryResponse resp = updateInventoryThresholdService.execute(storeId, variantId, 6);

        verify(inventoryJpaRepository).save(inventory);
        assertThat(inventory.getLowStockThreshold()).isEqualTo(6);
        assertThat(resp.lowStockThreshold()).isEqualTo(6);
    }

    @Test
    @DisplayName("UpdateInventoryThreshold: falla con INVENTORY_NOT_CONFIGURED si falta fila de inventario")
    void testUpdateThresholdFailsWhenInventoryNotConfigured() {
        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "250g", "SKU1",
                BigDecimal.TEN, "PEN", true, "ACTIVE", Instant.now(), Instant.now()
        );
        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));
        when(inventoryJpaRepository.findById(variantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateInventoryThresholdService.execute(storeId, variantId, 5))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INVENTORY_NOT_CONFIGURED"));
    }

    @Test
    @DisplayName("UpdateInventoryThreshold: permite lowStockThreshold=null para desactivar alertas")
    void testUpdateThresholdNullToDisableAlerts() {
        ProductVariantEntity variant = new ProductVariantEntity(
                variantId, storeId, productId, "250g", "SKU1",
                BigDecimal.TEN, "PEN", true, "ACTIVE", Instant.now(), Instant.now()
        );
        when(productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId))
                .thenReturn(Optional.of(variant));

        InventoryEntity inventory = new InventoryEntity(variantId, 20, 4, Instant.now());
        when(inventoryJpaRepository.findById(variantId)).thenReturn(Optional.of(inventory));

        StoreVariantInventoryProjection freshProjection = createProjection(true, 20, null);
        when(inventoryJpaRepository.findVariantInventoryByStoreIdAndVariantId(storeId, variantId))
                .thenReturn(Optional.of(freshProjection));

        InventoryResponse resp = updateInventoryThresholdService.execute(storeId, variantId, null);

        verify(inventoryJpaRepository).save(inventory);
        assertThat(inventory.getLowStockThreshold()).isNull();
        assertThat(resp.lowStockThreshold()).isNull();
    }
}
