package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.repository.ProductVariantJpaRepository;
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
public class UpdateInventoryStockService {

    private final ProductVariantJpaRepository productVariantJpaRepository;
    private final InventoryJpaRepository inventoryJpaRepository;
    private final GetVariantInventoryService getVariantInventoryService;

    public UpdateInventoryStockService(
            ProductVariantJpaRepository productVariantJpaRepository,
            InventoryJpaRepository inventoryJpaRepository,
            GetVariantInventoryService getVariantInventoryService
    ) {
        this.productVariantJpaRepository = productVariantJpaRepository;
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.getVariantInventoryService = getVariantInventoryService;
    }

    /**
     * Ajusta la cantidad física absoluta de una variante.
     * <p>
     * <strong>Semántica:</strong> Este método establece un valor físico absoluto (no delta incremental).
     * <br>
     * <strong>Concurrencia:</strong> Adquiere lock pesimista sobre ProductVariantEntity como ancla para sincronizar
     * inicializaciones y evitar inserciones huérfanas o carreras. Ejecuta upsert nativo atómico preservando
     * el low_stock_threshold si la fila ya existía.
     * <br>
     * <strong>Validaciones:</strong>
     * - tracksInventory debe ser true (si false -> BusinessRuleException).
     * - quantity no puede ser menor que las reservas activas vigentes (si menor -> ConflictException).
     * </p>
     */
    public InventoryResponse execute(UUID storeId, UUID variantId, int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("La cantidad de inventario no puede ser negativa.");
        }

        // 1. Lock ancla pesimista en la variante dentro del tenant
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

        Instant now = Instant.now();

        // 2. Validar que la nueva cantidad física no viole los holds activos vigentes
        int activeReserved = inventoryJpaRepository.sumActiveReservedQuantity(storeId, variantId, now);
        if (newQuantity < activeReserved) {
            throw new ConflictException(
                    "CANNOT_REDUCE_BELOW_RESERVED",
                    "No se puede ajustar la cantidad física a " + newQuantity +
                            " porque existen " + activeReserved + " unidades reservadas activas en proceso de compra."
            );
        }

        // 3. Upsert atómico nativo: inserta si no existe, o actualiza quantity manteniendo low_stock_threshold
        inventoryJpaRepository.upsertStock(variantId, newQuantity, now);

        // 4. Retornar vista fresca
        return getVariantInventoryService.execute(storeId, variantId);
    }
}

