package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.StoreStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminStoreDetailResponse(
        UUID id,
        UUID merchantUserId,
        String name,
        String slug,
        String description,
        String contactPhone,
        String address,
        String addressReference,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean pickupEnabled,
        boolean deliveryEnabled,
        BigDecimal deliveryFeeAmount,
        String deliveryFeeCurrency,
        boolean taxApplies,
        BigDecimal taxRate,
        StoreStatus status,
        Instant createdAt,
        Instant updatedAt,
        AdminStoreOwnerResponse merchant
) {}
