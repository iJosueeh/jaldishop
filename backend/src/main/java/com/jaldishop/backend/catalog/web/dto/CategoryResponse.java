package com.jaldishop.backend.catalog.web.dto;

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
) {}