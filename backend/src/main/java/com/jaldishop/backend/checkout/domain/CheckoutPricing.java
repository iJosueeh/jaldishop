package com.jaldishop.backend.checkout.domain;

import java.math.BigDecimal;

public record CheckoutPricing(
        BigDecimal productsSubtotalAmount,
        BigDecimal discountAmount,
        String discountCode,
        BigDecimal deliveryFeeAmount,
        BigDecimal taxRate,
        BigDecimal includedTaxAmount,
        BigDecimal totalAmount,
        String currency
) {}
