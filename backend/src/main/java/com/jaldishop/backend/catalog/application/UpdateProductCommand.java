package com.jaldishop.backend.catalog.application;

import java.util.UUID;

public record UpdateProductCommand(
        UUID productId,
        UUID storeId,
        UUID categoryId,
        String name,
        String slug,
        String description,
        String imageUrl,
        String status
) {}