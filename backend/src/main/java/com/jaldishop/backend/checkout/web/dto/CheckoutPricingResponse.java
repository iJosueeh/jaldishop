package com.jaldishop.backend.checkout.web.dto;

import java.math.BigDecimal;

public record CheckoutPricingResponse(
        BigDecimal productsSubtotalAmount,
        BigDecimal discountAmount,
        String discountCode,
        BigDecimal deliveryFeeAmount,
        BigDecimal taxRate,
        BigDecimal includedTaxAmount,
        BigDecimal totalAmount,
        String currency
) {}
