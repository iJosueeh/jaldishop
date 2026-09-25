package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateProductRequest(
        @NotNull(message = "El ID de la categoría es obligatorio")
        UUID categoryId,

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 160, message = "El nombre del producto no debe exceder 160 caracteres")
        String name,

        @Size(max = 180, message = "El slug del producto no debe exceder 180 caracteres")
        String slug,

        String description,

        String imageUrl,

        String status
) {}