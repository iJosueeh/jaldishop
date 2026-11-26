package com.jaldishop.backend.promo.application.dto;

import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PromoCodeResponse(
        UUID id,
        String code,
        PromoScope scope,
        PromoFundingSource fundingSource,
        UUID storeId,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer usageLimit,
        int usedCount,
        Instant startsAt,
        Instant endsAt,
        boolean active,
        boolean showInBanner,
        String bannerText,
        Instant createdAt,
        Instant updatedAt
) {
    public static PromoCodeResponse fromDomain(PromoCode promo) {
        return new PromoCodeResponse(
                promo.getId(),
                promo.getCode(),
                promo.getScope(),
                promo.getFundingSource(),
                promo.getStoreId(),
                promo.getDiscountType(),
                promo.getDiscountValue(),
                promo.getMinOrderAmount(),
                promo.getMaxDiscountAmount(),
                promo.getUsageLimit(),
                promo.getUsedCount(),
                promo.getStartsAt(),
                promo.getEndsAt(),
                promo.isActive(),
                promo.isShowInBanner(),
                promo.getBannerText(),
                promo.getCreatedAt(),
                promo.getUpdatedAt()
        );
    }
}
