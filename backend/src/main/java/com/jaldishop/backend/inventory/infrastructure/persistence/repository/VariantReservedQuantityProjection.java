package com.jaldishop.backend.inventory.infrastructure.persistence.repository;

import java.util.UUID;

public interface VariantReservedQuantityProjection {
    UUID getVariantId();
    Integer getReservedQuantity();
}
