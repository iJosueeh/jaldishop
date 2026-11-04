package com.jaldishop.backend.checkout.application.result;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderConfirmation(
        UUID orderId,
        String orderNumber,
        OrderStatus status,
        UUID storeId,
        String storeName,
        String customerName,
        BigDecimal totalAmount,
        String currency,
        Instant confirmedAt
) {}
