package com.jaldishop.backend.promo.web.dto;

import jakarta.validation.constraints.NotNull;

public record TogglePromoCodeRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean active
) {
}
