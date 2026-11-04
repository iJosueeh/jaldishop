package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.mapper.InventoryResponseMapper;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetVariantInventoryService {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryResponseMapper responseMapper;

    public GetVariantInventoryService(
            InventoryJpaRepository inventoryJpaRepository,
            InventoryResponseMapper responseMapper
    ) {
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.responseMapper = responseMapper;
    }

    public InventoryResponse execute(UUID storeId, UUID variantId) {
        StoreVariantInventoryProjection projection = inventoryJpaRepository
                .findVariantInventoryByStoreIdAndVariantId(storeId, variantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "VARIANT_NOT_FOUND",
                        "Variante de producto no encontrada o no pertenece a la tienda: " + variantId
                ));

        int reserved = 0;
        if (projection.isTracksInventory() && projection.getPhysicalQuantity() != null) {
            reserved = inventoryJpaRepository.sumActiveReservedQuantity(storeId, variantId, Instant.now());
        }

        return responseMapper.toResponse(projection, reserved);
    }
}
