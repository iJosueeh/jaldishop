package com.jaldishop.backend.promo.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidatePromoRequest(
        @NotNull(message = "La tienda es obligatoria")
        UUID storeId,

        @NotBlank(message = "El código promocional es obligatorio")
        @Size(max = 80, message = "El código es demasiado largo")
        String code,

        @NotNull(message = "El subtotal es obligatorio")
        @DecimalMin(value = "0.00", message = "El subtotal no puede ser negativo")
        BigDecimal subtotal
) {
}
