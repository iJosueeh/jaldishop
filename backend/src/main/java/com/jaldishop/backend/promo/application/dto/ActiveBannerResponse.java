package com.jaldishop.backend.promo.application.dto;

import com.jaldishop.backend.promo.domain.enums.DiscountType;

import java.math.BigDecimal;

public record ActiveBannerResponse(
        boolean active,
        String code,
        String bannerText,
        DiscountType discountType,
        BigDecimal discountValue
) {
    public static ActiveBannerResponse empty() {
        return new ActiveBannerResponse(false, null, null, null, null);
    }
}
