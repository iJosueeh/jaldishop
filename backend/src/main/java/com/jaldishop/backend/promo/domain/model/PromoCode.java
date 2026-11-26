package com.jaldishop.backend.promo.domain.model;

import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.shared.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class PromoCode {

    public static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{3,40}$");

    private final UUID id;
    private String code;
    private final PromoScope scope;
    private final PromoFundingSource fundingSource;
    private final UUID storeId;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private int usedCount;
    private Instant startsAt;
    private Instant endsAt;
    private boolean active;
    private boolean showInBanner;
    private String bannerText;
    private final Instant createdAt;
    private Instant updatedAt;

    public PromoCode(
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
        this.id = id != null ? id : UUID.randomUUID();
        this.scope = scope;
        this.fundingSource = fundingSource;
        this.storeId = storeId;
        this.usedCount = Math.max(0, usedCount);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        applyDetails(code, discountType, discountValue, minOrderAmount, maxDiscountAmount,
                usageLimit, startsAt, endsAt, active, showInBanner, bannerText);
        validateScopeInvariants();
    }

    public static String normalizeCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new BusinessRuleException("PROMO_CODE_REQUIRED", "El código promocional es obligatorio.");
        }
        String normalized = rawCode.trim().toUpperCase(Locale.ROOT);
        if (!CODE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessRuleException(
                    "PROMO_CODE_INVALID_FORMAT",
                    "El código debe tener entre 3 y 40 caracteres alfanuméricos, guiones o guiones bajos (sin espacios ni múltiples códigos)."
            );
        }
        return normalized;
    }

    public void update(
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
            String bannerText,
            Instant now
    ) {
        applyDetails(code, discountType, discountValue, minOrderAmount, maxDiscountAmount,
                usageLimit, startsAt, endsAt, active, showInBanner, bannerText);
        validateScopeInvariants();
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void toggleActive(boolean active, Instant now) {
        this.active = active;
        if (!active) {
            this.showInBanner = false;
        }
        this.updatedAt = now != null ? now : Instant.now();
    }

    public void clearBanner(Instant now) {
        this.showInBanner = false;
        this.updatedAt = now != null ? now : Instant.now();
    }

    private void applyDetails(
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
        this.code = normalizeCode(code);
        if (discountType == null) {
            throw new BusinessRuleException("PROMO_TYPE_REQUIRED", "El tipo de descuento es obligatorio.");
        }
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("PROMO_VALUE_INVALID", "El valor del descuento debe ser mayor a cero.");
        }
        if (discountType == DiscountType.PERCENTAGE && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessRuleException("PROMO_PERCENTAGE_INVALID", "El descuento porcentual no puede superar el 100%.");
        }
        if (discountType == DiscountType.FIXED_AMOUNT && maxDiscountAmount != null) {
            throw new BusinessRuleException("PROMO_MAX_DISCOUNT_NOT_ALLOWED", "El tope máximo de descuento solo aplica a cupones porcentuales.");
        }
        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("PROMO_MAX_DISCOUNT_INVALID", "El tope máximo de descuento debe ser mayor a cero.");
        }
        if (minOrderAmount != null && minOrderAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("PROMO_MIN_ORDER_INVALID", "El monto mínimo de compra no puede ser negativo.");
        }
        if (usageLimit != null && usageLimit <= 0) {
            throw new BusinessRuleException("PROMO_USAGE_LIMIT_INVALID", "El límite de usos debe ser mayor a cero.");
        }
        if (usageLimit != null && usageLimit < this.usedCount) {
            throw new BusinessRuleException("PROMO_USAGE_LIMIT_BELOW_USED", "El límite de usos no puede ser menor a los usos ya consumidos (" + this.usedCount + ").");
        }
        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new BusinessRuleException("PROMO_DATE_RANGE_INVALID", "La fecha de fin debe ser posterior a la fecha de inicio.");
        }

        String sanitizedBanner = bannerText != null ? bannerText.trim() : null;
        if (sanitizedBanner != null && sanitizedBanner.isEmpty()) {
            sanitizedBanner = null;
        }
        if (sanitizedBanner != null && sanitizedBanner.length() > 240) {
            throw new BusinessRuleException("PROMO_BANNER_TEXT_TOO_LONG", "El texto del banner no puede exceder 240 caracteres.");
        }
        if (sanitizedBanner != null && (sanitizedBanner.contains("<") || sanitizedBanner.contains(">"))) {
            throw new BusinessRuleException("PROMO_BANNER_TEXT_INVALID", "El texto del banner debe ser texto plano sin etiquetas HTML.");
        }
        if (showInBanner && sanitizedBanner == null) {
            throw new BusinessRuleException("PROMO_BANNER_TEXT_REQUIRED", "El texto del banner es obligatorio cuando se muestra en el marketplace.");
        }

        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.usageLimit = usageLimit;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = active;
        this.showInBanner = showInBanner;
        this.bannerText = sanitizedBanner;
    }

    private void validateScopeInvariants() {
        if (scope == null || fundingSource == null) {
            throw new BusinessRuleException("PROMO_SCOPE_REQUIRED", "El alcance del código promocional es obligatorio.");
        }
        if (scope == PromoScope.GLOBAL) {
            if (storeId != null) {
                throw new BusinessRuleException("PROMO_GLOBAL_STORE_FORBIDDEN", "Un código global no puede estar asociado a una tienda.");
            }
            if (fundingSource != PromoFundingSource.PLATFORM) {
                throw new BusinessRuleException("PROMO_GLOBAL_FUNDING_INVALID", "Un código global debe financiarse por la plataforma.");
            }
        } else {
            if (storeId == null) {
                throw new BusinessRuleException("PROMO_STORE_REQUIRED", "Un cupón de tienda requiere el identificador de la tienda.");
            }
            if (fundingSource != PromoFundingSource.MERCHANT) {
                throw new BusinessRuleException("PROMO_STORE_FUNDING_INVALID", "Un cupón de tienda debe financiarse por el comercio.");
            }
            if (showInBanner) {
                throw new BusinessRuleException("PROMO_STORE_BANNER_FORBIDDEN", "Los cupones de tienda no pueden publicarse en el banner global del marketplace.");
            }
        }
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
