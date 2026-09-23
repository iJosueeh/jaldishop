package com.jaldishop.backend.catalog.web.dto;

import com.jaldishop.backend.catalog.domain.Product;

import java.time.Instant;
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
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse fromDomain(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getStoreId(),
                product.getCategoryId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus().name(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}