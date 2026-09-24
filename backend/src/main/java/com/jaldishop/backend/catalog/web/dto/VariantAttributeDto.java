package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VariantAttributeDto(
        @NotBlank(message = "Attribute name is required")
        String name,

        @NotBlank(message = "Attribute value is required")
        String value
) {}