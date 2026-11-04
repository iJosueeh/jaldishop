package com.jaldishop.backend.ordering.application.result;

import com.jaldishop.backend.ordering.domain.model.OrderStatusHistory;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record MerchantOrderStatusHistoryResponse(
        UUID id,
        UUID changedByUserId,
        OrderStatus status,
        String reason,
        Instant changedAt
) {}
