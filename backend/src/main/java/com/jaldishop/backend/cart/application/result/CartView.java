package com.jaldishop.backend.cart.application.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartView(
        UUID id,
        UUID userId,
        UUID storeId,
        List<CartItemView> items,
        int totalItems,
        BigDecimal totalAmount,
        String currency,
        Instant updatedAt
) {}
