package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateStoreCommand(
        UUID merchantUserId,
        String name,
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
