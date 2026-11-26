package com.jaldishop.backend.promo.web.dto;

import com.jaldishop.backend.promo.application.dto.PromoCodeCommand;
import com.jaldishop.backend.promo.domain.enums.DiscountType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record UpsertPromoCodeRequest(
        @NotBlank(message = "El código promocional es obligatorio")
        @Size(min = 3, max = 40, message = "El código debe tener entre 3 y 40 caracteres")
        String code,

        @NotNull(message = "El tipo de descuento es obligatorio")
        DiscountType discountType,

        @NotNull(message = "El valor del descuento es obligatorio")
        @DecimalMin(value = "0.01", message = "El valor debe ser mayor a 0")
        BigDecimal discountValue,

        @DecimalMin(value = "0.00", message = "El monto mínimo no puede ser negativo")
        BigDecimal minOrderAmount,

        @DecimalMin(value = "0.01", message = "El tope máximo debe ser mayor a 0")
        BigDecimal maxDiscountAmount,

        @Min(value = 1, message = "El límite de usos debe ser mayor a 0")
        Integer usageLimit,

        Instant startsAt,
        Instant endsAt,
        Boolean active,
        Boolean showInBanner,

        @Size(max = 240, message = "El texto del banner no debe superar 240 caracteres")
        String bannerText
) {
    public PromoCodeCommand toCommand() {
        return new PromoCodeCommand(
                code,
                discountType,
                discountValue,
                minOrderAmount,
                maxDiscountAmount,
                usageLimit,
                startsAt,
                endsAt,
                active != null ? active : true,
                showInBanner != null && showInBanner,
                bannerText
        );
    }
}
