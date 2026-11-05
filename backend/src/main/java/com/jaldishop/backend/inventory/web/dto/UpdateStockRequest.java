package com.jaldishop.backend.inventory.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateStockRequest(
        @NotNull(message = "La cantidad física es requerida")
        @Min(value = 0, message = "La cantidad física no puede ser negativa")
        Integer quantity
) {
}
