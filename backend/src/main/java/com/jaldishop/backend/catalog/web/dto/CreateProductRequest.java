package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotBlank(message = "Product name is required")
        @Size(max = 160, message = "Product name must not exceed 160 characters")
        String name,

        @Size(max = 180, message = "Product slug must not exceed 180 characters")
        String slug,

        String description,

        String imageUrl
) {}