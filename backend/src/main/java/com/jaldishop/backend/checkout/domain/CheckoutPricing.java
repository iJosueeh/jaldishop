package com.jaldishop.backend.checkout.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record CheckoutPricing(
        BigDecimal productsSubtotalAmount,
        BigDecimal discountAmount,
        String discountCode,
        BigDecimal deliveryFeeAmount,
        BigDecimal taxRate,
        BigDecimal includedTaxAmount,
        BigDecimal totalAmount,
        String currency
) {

    public static CheckoutPricing calculate(
            List<CheckoutItemSnapshot> items,
            CheckoutFulfillmentType fulfillmentType,
            BigDecimal storeDeliveryFee,
            BigDecimal storeTaxRate,
            String currency
    ) {
        BigDecimal productsSubtotal = (items != null) ? items.stream()
                .map(CheckoutItemSnapshot::subtotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (fulfillmentType == CheckoutFulfillmentType.DELIVERY && storeDeliveryFee != null) {
            deliveryFee = storeDeliveryFee;
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        String discountCode = null;

        BigDecimal taxRate = storeTaxRate != null ? storeTaxRate : new BigDecimal("18.00");
        BigDecimal includedTaxAmount = BigDecimal.ZERO;

        if (taxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal denominator = BigDecimal.valueOf(100).add(taxRate);
            includedTaxAmount = productsSubtotal.multiply(taxRate).divide(denominator, 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = productsSubtotal.add(deliveryFee).subtract(discountAmount);
        String effectiveCurrency = (currency != null && !currency.isBlank()) ? currency : "PEN";

        return new CheckoutPricing(
                productsSubtotal,
                discountAmount,
                discountCode,
                deliveryFee,
                taxRate,
                includedTaxAmount,
                totalAmount,
                effectiveCurrency
        );
    }
}
