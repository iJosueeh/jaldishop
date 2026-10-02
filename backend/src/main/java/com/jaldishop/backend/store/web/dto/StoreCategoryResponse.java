package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.StoreCategory;
import com.jaldishop.backend.store.domain.StoreCategoryStatus;

import java.util.UUID;

public record StoreCategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        StoreCategoryStatus status
) {
    public static StoreCategoryResponse fromDomain(StoreCategory category) {
        if (category == null) return null;
        return new StoreCategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getStatus()
        );
    }
}
