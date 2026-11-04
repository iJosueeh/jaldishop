package com.jaldishop.backend.inventory.web.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryResponse(
        UUID variantId,
        UUID productId,
        String productName,
        String presentationName,
        String sku,
        boolean tracksInventory,
        String status,
        String inventoryStatus,
        Integer physicalQuantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer lowStockThreshold,
        boolean isLowStock,
        Instant updatedAt
) {
}
