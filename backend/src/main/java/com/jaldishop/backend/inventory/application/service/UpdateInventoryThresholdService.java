package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductVariantJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class UpdateInventoryThresholdService {

    private final ProductVariantJpaRepository productVariantJpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final GetVariantInventoryService getVariantInventoryService;

    public UpdateInventoryThresholdService(
            ProductVariantJpaRepository productVariantJpaRepository,
            InventoryJpaRepository inventoryJpaRepository,
            GetVariantInventoryService getVariantInventoryService
    ) {
        this.productVariantJpaRepository = productVariantJpaRepository;
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.getVariantInventoryService = getVariantInventoryService;
    }

    /**
     * Actualiza el umbral de alerta de stock bajo (low_stock_threshold).
     * <p>
     * <strong>Semántica:</strong> Permite valores enteros >= 0, o null para desactivar la alerta.
     * <br>
     * <strong>Requisitos:</strong>
     * - La variante debe pertenecer a la tienda.
     * - tracksInventory debe ser true.
     * - El inventario debe estar PREVIAMENTE configurado (existir fila en inventories).
     *   Si falta, arroja ConflictException ("INVENTORY_NOT_CONFIGURED").
     * </p>
     */
    public InventoryResponse execute(UUID storeId, UUID variantId, Integer lowStockThreshold) {
        if (lowStockThreshold != null && lowStockThreshold < 0) {
            throw new IllegalArgumentException("El umbral de stock bajo no puede ser negativo.");
        }

        // 1. Lock ancla en la variante
        ProductVariantEntity variant = productVariantJpaRepository.findByIdAndStoreIdForUpdate(variantId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "VARIANT_NOT_FOUND",
                        "Variante de producto no encontrada o no pertenece a la tienda: " + variantId
                ));

        if (!variant.isTracksInventory()) {
            throw new BusinessRuleException(
                    "INVENTORY_TRACKING_DISABLED",
                    "La variante no tiene activado el seguimiento de inventario (tracksInventory=false)."
            );
        }

        // 2. Verificar que exista fila de inventario (no se puede poner threshold sin haber inicializado stock)
        InventoryEntity inventory = inventoryJpaRepository.findById(variantId)
                .orElseThrow(() -> new ConflictException(
                        "INVENTORY_NOT_CONFIGURED",
                        "No se puede actualizar el umbral de alerta porque la variante no tiene inventario configurado. Inicialice primero el stock con PUT /stock."
                ));

        // 3. Actualizar únicamente el umbral y fecha
        inventory.setLowStockThreshold(lowStockThreshold);
        inventory.setUpdatedAt(Instant.now());
        inventoryJpaRepository.save(inventory);

        return getVariantInventoryService.execute(storeId, variantId);
    }
}

