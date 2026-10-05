package com.jaldishop.backend.catalog.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        UUID storeId,
        UUID categoryId,
        String name,
        String slug,
        String description,
        String imageUrl,
        String status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<ProductVariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {
    public ProductResponse(
            UUID id,
            UUID storeId,
            UUID categoryId,
            String name,
            String slug,
            String description,
            String imageUrl,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, storeId, categoryId, name, slug, description, imageUrl, status, null, null, null, createdAt, updatedAt);
    }
}