package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCatalogStatusRequest(
        @NotBlank(message = "El estado es obligatorio")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "El estado debe ser ACTIVE o INACTIVE")
        String status
) {}
