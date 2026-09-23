package com.jaldishop.backend.catalog.application;

import java.util.UUID;

public record CreateProductCommand(
        UUID storeId,
        UUID categoryId,
        String name,
        String slug,
        String description,
        String imageUrl
) {}