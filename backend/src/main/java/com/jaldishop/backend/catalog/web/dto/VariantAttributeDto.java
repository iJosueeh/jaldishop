package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VariantAttributeDto(
        @NotBlank(message = "El nombre del atributo es obligatorio")
        @Size(max = 120, message = "El nombre del atributo no debe exceder 120 caracteres")
        String name,

        @NotBlank(message = "El valor del atributo es obligatorio")
        String value
) {}