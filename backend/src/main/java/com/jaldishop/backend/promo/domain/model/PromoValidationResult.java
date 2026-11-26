package com.jaldishop.backend.promo.domain.model;

import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;

import java.math.BigDecimal;
import java.util.UUID;

public record PromoValidationResult(
        boolean valid,
        UUID promoCodeId,
        String code,
        PromoFundingSource fundingSource,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal newSubtotal,
        String errorCode,
        String errorMessage
) {
    public static PromoValidationResult valid(
            PromoCode promo,
            BigDecimal discountAmount,
            BigDecimal newSubtotal
    ) {
        return new PromoValidationResult(
                true,
                promo.getId(),
                promo.getCode(),
                promo.getFundingSource(),
                promo.getDiscountType(),
                promo.getDiscountValue(),
                discountAmount,
                newSubtotal,
                null,
                null
        );
    }

    public static PromoValidationResult invalid(String code, String errorCode, String errorMessage, BigDecimal subtotal) {
        return new PromoValidationResult(
                false,
                null,
                code,
                null,
                null,
                null,
                BigDecimal.ZERO,
                subtotal != null ? subtotal : BigDecimal.ZERO,
                errorCode,
                errorMessage
        );
    }
}
