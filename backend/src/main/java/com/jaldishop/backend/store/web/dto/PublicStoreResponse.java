package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreStatus;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record PublicStoreResponse(
        UUID id,
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
        String logoUrl,
        String bannerUrl,
        String instagramUrl,
        String facebookUrl,
        String whatsappNumber,
        Set<UUID> categoryIds,
        StoreStatus status
) {
    public static PublicStoreResponse fromDomain(Store store) {
        if (store == null) {
            return null;
        }
        return new PublicStoreResponse(
                store.getId(),
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
                store.getLogoUrl(),
                store.getBannerUrl(),
                store.getInstagramUrl(),
                store.getFacebookUrl(),
                store.getWhatsappNumber(),
                store.getCategoryIds(),
                store.getStatus()
        );
    }
}
