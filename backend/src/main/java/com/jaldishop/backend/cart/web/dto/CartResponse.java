package com.jaldishop.backend.cart.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        UUID storeId,
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount,
        String currency,
        Instant updatedAt
) {}
