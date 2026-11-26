package com.jaldishop.backend.promo.application.dto;

import com.jaldishop.backend.promo.domain.enums.DiscountType;

import java.math.BigDecimal;
import java.time.Instant;

public record PromoCodeCommand(
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer usageLimit,
        Instant startsAt,
        Instant endsAt,
        boolean active,
        boolean showInBanner,
        String bannerText
) {
}
