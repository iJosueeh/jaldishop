package com.jaldishop.backend.catalog.application.command;

import java.util.UUID;

public record CreateCategoryCommand(
        UUID storeId,
        String name,
        String description
) {}