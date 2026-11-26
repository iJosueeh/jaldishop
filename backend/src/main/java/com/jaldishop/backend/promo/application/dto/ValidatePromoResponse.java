package com.jaldishop.backend.promo.application.dto;

import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.model.PromoValidationResult;

import java.math.BigDecimal;

public record ValidatePromoResponse(
        boolean valid,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal newSubtotal,
        String errorCode,
        String message
) {
    public static ValidatePromoResponse fromResult(PromoValidationResult result) {
        return new ValidatePromoResponse(
                result.valid(),
                result.code(),
                result.discountType(),
                result.discountValue(),
                result.discountAmount(),
                result.newSubtotal(),
                result.errorCode(),
                result.errorMessage()
        );
    }
}
