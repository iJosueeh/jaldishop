package com.jaldishop.backend.shared.domain.model;

import java.math.BigDecimal;

public record MonetarySummary(
        BigDecimal subtotal,
        BigDecimal discount,
        String discountCode,
        BigDecimal deliveryFee,
        BigDecimal serviceFee,
        BigDecimal tax,
        BigDecimal total,
        String currency
) {
    public MonetarySummary(
            BigDecimal subtotal,
            BigDecimal discount,
            String discountCode,
            BigDecimal deliveryFee,
            BigDecimal tax,
            BigDecimal total,
            String currency
    ) {
        this(subtotal, discount, discountCode, deliveryFee, BigDecimal.ZERO, tax, total, currency);
    }

    public MonetarySummary {
        subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        discount = discount != null ? discount : BigDecimal.ZERO;
        discountCode = (discountCode != null && !discountCode.isBlank()) ? discountCode.trim() : null;
        deliveryFee = deliveryFee != null ? deliveryFee : BigDecimal.ZERO;
        serviceFee = serviceFee != null ? serviceFee : BigDecimal.ZERO;
        tax = tax != null ? tax : BigDecimal.ZERO;
        total = total != null ? total : BigDecimal.ZERO;
        currency = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "PEN";
    }

    public static MonetarySummary of(
            BigDecimal subtotal,
            BigDecimal discount,
            String discountCode,
            BigDecimal deliveryFee,
            BigDecimal tax,
            BigDecimal total,
            String currency
    ) {
        return new MonetarySummary(subtotal, discount, discountCode, deliveryFee, BigDecimal.ZERO, tax, total, currency);
    }

    public static MonetarySummary of(
            BigDecimal subtotal,
            BigDecimal discount,
            String discountCode,
            BigDecimal deliveryFee,
            BigDecimal serviceFee,
            BigDecimal tax,
            BigDecimal total,
            String currency
    ) {
        return new MonetarySummary(subtotal, discount, discountCode, deliveryFee, serviceFee, tax, total, currency);
    }

    public boolean hasDiscount() {
        return discount != null && discount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal calculateExpectedTotal() {
        return subtotal.subtract(discount).add(deliveryFee).add(serviceFee);
    }

    /**
     * Gross sales attributable to the merchant (excludes buyer-paid platform service fee).
     */
    public BigDecimal merchantGrossAmount() {
        return subtotal.subtract(discount).add(deliveryFee);
    }

    public Money subtotalMoney() {
        return Money.of(subtotal, currency);
    }

    public Money discountMoney() {
        return Money.of(discount, currency);
    }

    public Money deliveryFeeMoney() {
        return Money.of(deliveryFee, currency);
    }

    public Money serviceFeeMoney() {
        return Money.of(serviceFee, currency);
    }

    public Money taxMoney() {
        return Money.of(tax, currency);
    }

    public Money totalMoney() {
        return Money.of(total, currency);
    }

    public Money calculateExpectedTotalMoney() {
        return Money.of(calculateExpectedTotal(), currency);
    }
}
