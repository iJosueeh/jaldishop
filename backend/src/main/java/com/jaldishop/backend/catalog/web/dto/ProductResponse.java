package com.jaldishop.backend.catalog.web.dto;

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
) {}