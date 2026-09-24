package com.jaldishop.backend.store.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StoreCustomerResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phone,
        Instant customerSince,
        long ordersCount,
        BigDecimal totalSpentAmount,
        Instant lastOrderAt
) {}
