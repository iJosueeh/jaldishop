package com.jaldishop.backend.catalog.application;

import java.util.UUID;

public record UpdateCategoryCommand(
        UUID categoryId,
        UUID storeId,
        String name,
        String description,
        String status
) {}