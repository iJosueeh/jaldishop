package com.jaldishop.backend.checkout.domain.model;

import com.jaldishop.backend.checkout.domain.enums.CheckoutFulfillmentType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record CheckoutPricing(
        BigDecimal productsSubtotalAmount,
        BigDecimal discountAmount,
        String discountCode,
        BigDecimal deliveryFeeAmount,
        BigDecimal serviceFeeRate,
        BigDecimal serviceFeeAmount,
        BigDecimal taxRate,
        BigDecimal includedTaxAmount,
        BigDecimal totalAmount,
        String currency
) {
    public CheckoutPricing(
            BigDecimal productsSubtotalAmount,
            BigDecimal discountAmount,
            String discountCode,
            BigDecimal deliveryFeeAmount,
            BigDecimal taxRate,
            BigDecimal includedTaxAmount,
            BigDecimal totalAmount,
            String currency
    ) {
        this(
                productsSubtotalAmount,
                discountAmount,
                discountCode,
                deliveryFeeAmount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                taxRate,
                includedTaxAmount,
                totalAmount,
                currency
        );
    }

    public static CheckoutPricing calculate(
            List<CheckoutItemSnapshot> items,
            CheckoutFulfillmentType fulfillmentType,
            BigDecimal storeDeliveryFee,
            BigDecimal storeTaxRate,
            String currency
    ) {
        return calculate(items, fulfillmentType, storeDeliveryFee, storeTaxRate, BigDecimal.ZERO, null, currency);
    }

    public static CheckoutPricing calculate(
            List<CheckoutItemSnapshot> items,
            CheckoutFulfillmentType fulfillmentType,
            BigDecimal storeDeliveryFee,
            BigDecimal storeTaxRate,
            BigDecimal customerServiceFeeRate,
            String currency
    ) {
        return calculate(items, fulfillmentType, storeDeliveryFee, storeTaxRate, customerServiceFeeRate, null, currency);
    }

    public static CheckoutPricing calculate(
            List<CheckoutItemSnapshot> items,
            CheckoutFulfillmentType fulfillmentType,
            BigDecimal storeDeliveryFee,
            BigDecimal storeTaxRate,
            BigDecimal customerServiceFeeRate,
            BigDecimal customerServiceFeeCap,
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

        BigDecimal effectiveServiceFeeRate = (customerServiceFeeRate != null && customerServiceFeeRate.compareTo(BigDecimal.ZERO) > 0)
                ? customerServiceFeeRate
                : BigDecimal.ZERO;
        BigDecimal serviceFeeAmount = effectiveServiceFeeRate.compareTo(BigDecimal.ZERO) > 0
                ? productsSubtotal.multiply(effectiveServiceFeeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        if (customerServiceFeeCap != null && customerServiceFeeCap.compareTo(BigDecimal.ZERO) >= 0
                && serviceFeeAmount.compareTo(customerServiceFeeCap) > 0) {
            serviceFeeAmount = customerServiceFeeCap.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal taxRate = storeTaxRate != null ? storeTaxRate : new BigDecimal("18.00");
        BigDecimal includedTaxAmount = BigDecimal.ZERO;

        if (taxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal denominator = BigDecimal.valueOf(100).add(taxRate);
            includedTaxAmount = productsSubtotal.multiply(taxRate).divide(denominator, 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAmount = productsSubtotal.add(deliveryFee).add(serviceFeeAmount).subtract(discountAmount);
        String effectiveCurrency = (currency != null && !currency.isBlank()) ? currency : "PEN";

        return new CheckoutPricing(
                productsSubtotal,
                discountAmount,
                discountCode,
                deliveryFee,
                effectiveServiceFeeRate,
                serviceFeeAmount,
                taxRate,
                includedTaxAmount,
                totalAmount,
                effectiveCurrency
        );
    }
}
