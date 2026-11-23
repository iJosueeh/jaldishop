package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.VariantReservedQuantityProjection;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.mapper.InventoryResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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

        if (lowStockProjections.isEmpty()) {
            return List.of();
        }

        List<UUID> trackedVariantIds = lowStockProjections.stream()
                .filter(p -> p.isTracksInventory() && p.getPhysicalQuantity() != null)
                .map(StoreVariantInventoryProjection::getVariantId)
                .toList();

        Map<UUID, Integer> reservedMap;
        if (trackedVariantIds.isEmpty()) {
            reservedMap = Collections.emptyMap();
        } else {
            List<VariantReservedQuantityProjection> reservedProjections =
                    inventoryJpaRepository.sumActiveReservedQuantitiesByStoreIdAndVariantIds(storeId, trackedVariantIds, now);
            reservedMap = reservedProjections.stream()
                    .collect(Collectors.toMap(
                            VariantReservedQuantityProjection::getVariantId,
                            p -> p.getReservedQuantity() != null ? p.getReservedQuantity() : 0,
                            (existing, replacement) -> existing
                    ));
        }

        return lowStockProjections.stream()
                .map(projection -> {
                    int reserved = 0;
                    if (projection.isTracksInventory() && projection.getPhysicalQuantity() != null) {
                        reserved = reservedMap.getOrDefault(projection.getVariantId(), 0);
                    }
                    return responseMapper.toResponse(projection, reserved);
                })
                .toList();
    }
}

