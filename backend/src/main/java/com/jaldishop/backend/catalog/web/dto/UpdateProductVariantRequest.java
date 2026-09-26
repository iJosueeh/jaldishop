package com.jaldishop.backend.catalog.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductVariantRequest(
        @NotBlank(message = "El nombre de presentación es obligatorio")
        @Size(max = 120, message = "El nombre de presentación no debe exceder 120 caracteres")
        String presentationName,

        @Size(max = 100, message = "El SKU no debe exceder 100 caracteres")
        String sku,

        @NotNull(message = "El monto del precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto del precio debe ser mayor a cero")
        BigDecimal priceAmount,

        @NotBlank(message = "La moneda del precio es obligatoria")
        @Pattern(regexp = "^[A-Z]{3}$", message = "La moneda debe ser un código ISO de 3 letras en mayúsculas")
        String priceCurrency,

        boolean tracksInventory,

        String status,

        @Valid
        List<VariantAttributeDto> attributes
) {}