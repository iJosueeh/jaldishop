package com.jaldishop.backend.cart.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateCartItemRequest(
        @NotNull(message = "El ID de la tienda es obligatorio")
        UUID storeId,

        @Min(value = 0, message = "La cantidad no puede ser negativa")
        int quantity
) {}
