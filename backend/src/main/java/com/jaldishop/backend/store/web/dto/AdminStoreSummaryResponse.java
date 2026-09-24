package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.StoreStatus;

import java.time.Instant;
import java.util.UUID;

public record AdminStoreSummaryResponse(
        UUID id,
        UUID merchantUserId,
        String name,
        String slug,
        String contactPhone,
        StoreStatus status,
        boolean deliveryEnabled,
        boolean pickupEnabled,
        Instant createdAt,
        Instant updatedAt,
        AdminStoreOwnerResponse merchant
) {}
