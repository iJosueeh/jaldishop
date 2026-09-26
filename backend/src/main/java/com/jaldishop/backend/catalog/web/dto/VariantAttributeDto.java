package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;

public record VariantAttributeDto(
        @NotBlank(message = "El nombre del atributo es obligatorio")
        String name,

        @NotBlank(message = "El valor del atributo es obligatorio")
        String value
) {}