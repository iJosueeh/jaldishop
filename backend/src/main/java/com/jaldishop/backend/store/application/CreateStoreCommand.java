package com.jaldishop.backend.store.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateStoreCommand(
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
        BigDecimal taxRate
) {}
