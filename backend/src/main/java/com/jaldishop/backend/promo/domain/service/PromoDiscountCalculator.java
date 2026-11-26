package com.jaldishop.backend.promo.domain.service;

import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.model.PromoValidationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class PromoDiscountCalculator {

    private final Clock clock;

    public PromoDiscountCalculator(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public PromoValidationResult validateAndCalculate(PromoCode promo, UUID storeId, BigDecimal productsSubtotal) {
        Objects.requireNonNull(storeId, "storeId es obligatorio");
        BigDecimal safeSubtotal = (productsSubtotal != null && productsSubtotal.compareTo(BigDecimal.ZERO) > 0)
                ? productsSubtotal.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (promo == null) {
            return PromoValidationResult.invalid(null, "PROMO_NOT_FOUND", "El código promocional no existe.", safeSubtotal);
        }

        if (!promo.isActive()) {
            return PromoValidationResult.invalid(promo.getCode(), "PROMO_INACTIVE", "Este código promocional no se encuentra activo.", safeSubtotal);
        }

        Instant now = clock.instant();
        if (promo.getStartsAt() != null && now.isBefore(promo.getStartsAt())) {
            return PromoValidationResult.invalid(promo.getCode(), "PROMO_NOT_STARTED", "Este código promocional aún no inicia su vigencia.", safeSubtotal);
        }
        if (promo.getEndsAt() != null && now.isAfter(promo.getEndsAt())) {
            return PromoValidationResult.invalid(promo.getCode(), "PROMO_EXPIRED", "Este código promocional ha expirado.", safeSubtotal);
        }

        if (promo.getScope() == PromoScope.STORE && !storeId.equals(promo.getStoreId())) {
            return PromoValidationResult.invalid(promo.getCode(), "PROMO_WRONG_STORE", "Este cupón no es válido para esta tienda.", safeSubtotal);
        }

        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            return PromoValidationResult.invalid(promo.getCode(), "PROMO_EXHAUSTED", "Este código promocional alcanzó su límite máximo de usos.", safeSubtotal);
        }

        if (promo.getMinOrderAmount() != null && safeSubtotal.compareTo(promo.getMinOrderAmount()) < 0) {
            return PromoValidationResult.invalid(
                    promo.getCode(),
                    "PROMO_MIN_ORDER_NOT_MET",
                    "El pedido mínimo para este código es de S/ " + promo.getMinOrderAmount().setScale(2, RoundingMode.HALF_UP) + ".",
                    safeSubtotal
            );
        }

        BigDecimal discount;
        if (promo.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = safeSubtotal
                    .multiply(promo.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (promo.getMaxDiscountAmount() != null && discount.compareTo(promo.getMaxDiscountAmount()) > 0) {
                discount = promo.getMaxDiscountAmount().setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            discount = promo.getDiscountValue().setScale(2, RoundingMode.HALF_UP);
        }

        if (discount.compareTo(safeSubtotal) > 0) {
            discount = safeSubtotal;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal newSubtotal = safeSubtotal.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return PromoValidationResult.valid(promo, discount, newSubtotal);
    }
}
