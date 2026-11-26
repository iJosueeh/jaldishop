package com.jaldishop.backend.promo.domain.service;

import com.jaldishop.backend.promo.domain.enums.DiscountType;
import com.jaldishop.backend.promo.domain.enums.PromoFundingSource;
import com.jaldishop.backend.promo.domain.enums.PromoScope;
import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.model.PromoValidationResult;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromoDiscountCalculatorTest {

    private static final Instant NOW = Instant.parse("2026-11-25T15:00:00Z");
    private final PromoDiscountCalculator calculator = new PromoDiscountCalculator(Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID storeA = UUID.randomUUID();
    private final UUID storeB = UUID.randomUUID();

    @Test
    void calculatesPercentageWithMaxCap() {
        PromoCode promo = buildPromo("JALDI20", PromoScope.GLOBAL, PromoFundingSource.PLATFORM, null,
                DiscountType.PERCENTAGE, "20.00", "50.00", "15.00", 100, 10,
                NOW.minus(1, ChronoUnit.DAYS), NOW.plus(1, ChronoUnit.DAYS), true);

        PromoValidationResult result = calculator.validateAndCalculate(promo, storeA, new BigDecimal("100.00"));

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("15.00");
        assertThat(result.newSubtotal()).isEqualByComparingTo("85.00");
        assertThat(result.fundingSource()).isEqualTo(PromoFundingSource.PLATFORM);
    }

    @Test
    void clampsFixedDiscountToSubtotal() {
        PromoCode promo = buildPromo("FLAT50", PromoScope.STORE, PromoFundingSource.MERCHANT, storeA,
                DiscountType.FIXED_AMOUNT, "50.00", null, null, null, 0,
                null, null, true);

        PromoValidationResult result = calculator.validateAndCalculate(promo, storeA, new BigDecimal("32.50"));

        assertThat(result.valid()).isTrue();
        assertThat(result.discountAmount()).isEqualByComparingTo("32.50");
        assertThat(result.newSubtotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsStoreCouponOnDifferentStore() {
        PromoCode promo = buildPromo("TIENDA10", PromoScope.STORE, PromoFundingSource.MERCHANT, storeA,
                DiscountType.PERCENTAGE, "10.00", null, null, null, 0,
                null, null, true);

        PromoValidationResult result = calculator.validateAndCalculate(promo, storeB, new BigDecimal("80.00"));

        assertThat(result.valid()).isFalse();
        assertThat(result.errorCode()).isEqualTo("PROMO_WRONG_STORE");
        assertThat(result.discountAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsExpiredExhaustedAndMinOrderNotMet() {
        PromoCode expired = buildPromo("OLD10", PromoScope.GLOBAL, PromoFundingSource.PLATFORM, null,
                DiscountType.PERCENTAGE, "10.00", null, null, null, 0,
                NOW.minus(5, ChronoUnit.DAYS), NOW.minus(1, ChronoUnit.HOURS), true);
        assertThat(calculator.validateAndCalculate(expired, storeA, new BigDecimal("50.00")).errorCode())
                .isEqualTo("PROMO_EXPIRED");

        PromoCode exhausted = buildPromo("LIMIT1", PromoScope.GLOBAL, PromoFundingSource.PLATFORM, null,
                DiscountType.PERCENTAGE, "10.00", null, null, 5, 5,
                null, null, true);
        assertThat(calculator.validateAndCalculate(exhausted, storeA, new BigDecimal("50.00")).errorCode())
                .isEqualTo("PROMO_EXHAUSTED");

        PromoCode minOrder = buildPromo("MIN60", PromoScope.GLOBAL, PromoFundingSource.PLATFORM, null,
                DiscountType.FIXED_AMOUNT, "10.00", new BigDecimal("60.00").toString(), null, null, 0,
                null, null, true);
        assertThat(calculator.validateAndCalculate(minOrder, storeA, new BigDecimal("45.00")).errorCode())
                .isEqualTo("PROMO_MIN_ORDER_NOT_MET");
    }

    @Test
    void rejectsMultipleOrMalformedCodesInSingleField() {
        assertThatThrownBy(() -> PromoCode.normalizeCode("CUPON1,CUPON2"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> PromoCode.normalizeCode("CUPON1 CUPON2"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(PromoCode.normalizeCode("  bienvenido_10  ")).isEqualTo("BIENVENIDO_10");
    }

    private PromoCode buildPromo(
            String code, PromoScope scope, PromoFundingSource funding, UUID storeId,
            DiscountType type, String value, String minOrder, String maxDiscount,
            Integer usageLimit, int usedCount, Instant startsAt, Instant endsAt, boolean active
    ) {
        return new PromoCode(
                UUID.randomUUID(), code, scope, funding, storeId, type,
                new BigDecimal(value),
                minOrder != null ? new BigDecimal(minOrder) : null,
                maxDiscount != null ? new BigDecimal(maxDiscount) : null,
                usageLimit, usedCount, startsAt, endsAt, active, false, null, NOW, NOW
        );
    }
}
