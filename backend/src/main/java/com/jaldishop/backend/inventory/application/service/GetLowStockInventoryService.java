package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.mapper.InventoryResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetLowStockInventoryService {

    private final InventoryJpaRepository inventoryJpaRepository;
    private final InventoryResponseMapper responseMapper;

    public GetLowStockInventoryService(
            InventoryJpaRepository inventoryJpaRepository,
            InventoryResponseMapper responseMapper
    ) {
        this.inventoryJpaRepository = inventoryJpaRepository;
        this.responseMapper = responseMapper;
    }

    public List<InventoryResponse> execute(UUID storeId) {
        Instant now = Instant.now();
        List<StoreVariantInventoryProjection> lowStockProjections =
                inventoryJpaRepository.findLowStockVariantsByStoreId(storeId);

        return lowStockProjections.stream()
                .map(projection -> {
                    int reserved = 0;
                    if (projection.isTracksInventory() && projection.getPhysicalQuantity() != null) {
                        reserved = inventoryJpaRepository.sumActiveReservedQuantity(storeId, projection.getVariantId(), now);
                    }
                    return responseMapper.toResponse(projection, reserved);
                })
                .toList();
    }
}
