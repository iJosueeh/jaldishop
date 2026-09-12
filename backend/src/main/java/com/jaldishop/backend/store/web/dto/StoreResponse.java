package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StoreResponse(
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
        Instant updatedAt
) {
    public static StoreResponse fromDomain(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getMerchantUserId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getContactPhone(),
                store.getAddress(),
                store.getAddressReference(),
                store.getLatitude(),
                store.getLongitude(),
                store.isPickupEnabled(),
                store.isDeliveryEnabled(),
                store.getDeliveryFeeAmount(),
                store.getDeliveryFeeCurrency(),
                store.isTaxApplies(),
                store.getTaxRate(),
                store.getStatus(),
                store.getCreatedAt(),
                store.getUpdatedAt()
        );
    }
}
