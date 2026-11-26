package com.jaldishop.backend.promo.infrastructure.persistence.entity;

import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "promo_codes")
public class PromoCodeEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PromoScope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "funding_source", nullable = false, length = 20)
    private PromoFundingSource fundingSource;

    @Column(name = "store_id")
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "show_in_banner", nullable = false)
    private boolean showInBanner;

    @Column(name = "banner_text", length = 240)
    private String bannerText;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PromoCodeEntity() {
    }

    public PromoCodeEntity(
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
        this.id = id;
        this.code = code;
        this.scope = scope;
        this.fundingSource = fundingSource;
        this.storeId = storeId;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.usageLimit = usageLimit;
        this.usedCount = usedCount;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = active;
        this.showInBanner = showInBanner;
        this.bannerText = bannerText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public PromoScope getScope() { return scope; }
    public PromoFundingSource getFundingSource() { return fundingSource; }
    public UUID getStoreId() { return storeId; }
    public DiscountType getDiscountType() { return discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public BigDecimal getMinOrderAmount() { return minOrderAmount; }
    public BigDecimal getMaxDiscountAmount() { return maxDiscountAmount; }
    public Integer getUsageLimit() { return usageLimit; }
    public int getUsedCount() { return usedCount; }
    public Instant getStartsAt() { return startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public boolean isActive() { return active; }
    public boolean isShowInBanner() { return showInBanner; }
    public String getBannerText() { return bannerText; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
