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
        BigDecimal taxRate,
        String logoUrl,
        String bannerUrl,
        String instagramUrl,
        String facebookUrl,
        String whatsappNumber,
        StoreStatus status,
        Instant createdAt,
        Instant updatedAt,
        AdminStoreOwnerResponse merchant
) {
    public AdminStoreDetailResponse(
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
            BigDecimal taxRate,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt,
            AdminStoreOwnerResponse merchant
    ) {
        this(
                id,
                merchantUserId,
                name,
                slug,
                description,
                contactPhone,
                address,
                addressReference,
                latitude,
                longitude,
                pickupEnabled,
                deliveryEnabled,
                deliveryFeeAmount,
                deliveryFeeCurrency,
                taxRate,
                null,
                null,
                null,
                null,
                null,
                status,
                createdAt,
                updatedAt,
                merchant
        );
    }
}
