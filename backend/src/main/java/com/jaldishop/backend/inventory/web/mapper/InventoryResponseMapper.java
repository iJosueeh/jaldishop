package com.jaldishop.backend.inventory.web.mapper;

import com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import org.springframework.stereotype.Component;

@Component
public class InventoryResponseMapper {

    public InventoryResponse toResponse(StoreVariantInventoryProjection projection, int reservedQuantity) {
        if (projection == null) {
            return null;
        }

        boolean tracks = projection.isTracksInventory();
        Integer physical = projection.getPhysicalQuantity();
        Integer threshold = projection.getLowStockThreshold();

        String inventoryStatus;
        Integer reserved = null;
        Integer available = null;
        boolean lowStock = false;

        if (!tracks) {
            inventoryStatus = "NOT_TRACKED";
            physical = null;
            threshold = null;
        } else if (physical == null) {
            inventoryStatus = "NOT_CONFIGURED";
        } else {
            inventoryStatus = "CONFIGURED";
            reserved = reservedQuantity;
            available = Math.max(0, physical - reservedQuantity);
            lowStock = (threshold != null && physical <= threshold);
        }

        return new InventoryResponse(
                projection.getVariantId(),
                projection.getProductId(),
                projection.getProductName(),
                projection.getPresentationName(),
                projection.getSku(),
                tracks,
                projection.getStatus(),
                inventoryStatus,
                physical,
                reserved,
                available,
                threshold,
                lowStock,
                projection.getInventoryUpdatedAt()
        );
    }
}
