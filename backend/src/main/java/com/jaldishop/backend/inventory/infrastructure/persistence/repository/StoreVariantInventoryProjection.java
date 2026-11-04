package com.jaldishop.backend.inventory.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.UUID;

public interface StoreVariantInventoryProjection {
    UUID getVariantId();
    UUID getProductId();
    String getProductName();
    String getPresentationName();
    String getSku();
    boolean isTracksInventory();
    String getStatus();
    Integer getPhysicalQuantity();
    Integer getLowStockThreshold();
    Instant getInventoryUpdatedAt();
}
