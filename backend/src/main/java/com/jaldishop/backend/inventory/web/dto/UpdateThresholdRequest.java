package com.jaldishop.backend.inventory.web.dto;

import jakarta.validation.constraints.Min;

public record UpdateThresholdRequest(
        @Min(value = 0, message = "El umbral de stock bajo no puede ser negativo")
        Integer lowStockThreshold
) {
}
