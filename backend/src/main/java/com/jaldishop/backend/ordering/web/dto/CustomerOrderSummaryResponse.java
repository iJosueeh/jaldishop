package com.jaldishop.backend.ordering.web.dto;

import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CustomerOrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID storeId,
        String storeName,
        String storeSlug,
        String storeLogoUrl,
        OrderStatus status,
        DeliveryMode deliveryMode,
        LocalDate serviceDate,
        LocalTime serviceStartTime,
        LocalTime serviceEndTime,
        BigDecimal totalAmount,
        String currency,
        int totalItems,
        Instant confirmedAt
) {}
