package com.jaldishop.backend.inventory.domain.repository;

import com.jaldishop.backend.inventory.domain.model.Inventory;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    Optional<Inventory> findByVariantId(UUID variantId);
    Inventory save(Inventory inventory);
}
