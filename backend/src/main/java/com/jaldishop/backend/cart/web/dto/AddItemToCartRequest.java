package com.jaldishop.backend.cart.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddItemToCartRequest(
        @NotNull(message = "El ID de la tienda es obligatorio")
        UUID storeId,

        @NotNull(message = "El ID de la variante es obligatorio")
        UUID variantId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int quantity
) {}
