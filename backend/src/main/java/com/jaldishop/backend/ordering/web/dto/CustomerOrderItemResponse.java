package com.jaldishop.backend.ordering.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerOrderItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        String productName,
        String variantName,
        int quantity,
        BigDecimal unitPriceAmount,
        BigDecimal subtotalAmount
) {}
