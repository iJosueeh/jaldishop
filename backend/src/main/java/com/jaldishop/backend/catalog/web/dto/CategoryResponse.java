package com.jaldishop.backend.catalog.web.dto;

import com.jaldishop.backend.catalog.domain.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        UUID storeId,
        String name,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryResponse fromDomain(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getStoreId(),
                category.getName(),
                category.getDescription(),
                category.getStatus().name(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}