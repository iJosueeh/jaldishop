package com.jaldishop.backend.ordering.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMerchantOrderItemRequest(
        @NotNull(message = "El ID de la variante es obligatorio")
        UUID variantId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int quantity,

        BigDecimal unitPriceAmount
) {}
