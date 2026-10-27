package com.jaldishop.backend.ordering.web.dto;

import com.jaldishop.backend.ordering.domain.OrderStatus;
import java.time.Instant;

public record CustomerOrderStatusHistoryResponse(
        OrderStatus status,
        String reason,
        Instant changedAt
) {}
