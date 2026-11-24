package com.jaldishop.backend.checkout.web.dto;

import java.math.BigDecimal;

public record CheckoutPricingResponse(
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
    public CheckoutPricingResponse(
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
}
