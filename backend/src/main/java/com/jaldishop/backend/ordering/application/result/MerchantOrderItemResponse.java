package com.jaldishop.backend.ordering.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record MerchantOrderItemResponse(
        UUID id,
        UUID variantId,
        String productName,
        String variantName,
        int quantity,
        BigDecimal unitPriceAmount,
        BigDecimal subtotalAmount
) {}
