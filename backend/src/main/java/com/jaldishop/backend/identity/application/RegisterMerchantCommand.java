package com.jaldishop.backend.identity.application;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record RegisterMerchantCommand(
        String email,
        String password,
        String firstName,
        String lastName,
        String phone,
        String storeName,
        String businessType,
        String storeContactPhone,
        String address,
        String addressReference,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean pickupEnabled,
        boolean deliveryEnabled,
        String logoUrl,
        String bannerUrl,
        Set<UUID> categoryIds
) {
    public RegisterMerchantCommand(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            String storeName,
            String businessType,
            String storeContactPhone,
            String address,
            boolean pickupEnabled,
            boolean deliveryEnabled,
            String logoUrl,
            String bannerUrl,
            Set<UUID> categoryIds
    ) {
        this(
                email,
                password,
                firstName,
                lastName,
                phone,
                storeName,
                businessType,
                storeContactPhone,
                address,
                null,
                null,
                null,
                pickupEnabled,
                deliveryEnabled,
                logoUrl,
                bannerUrl,
                categoryIds
        );
    }

    public RegisterMerchantCommand(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            String storeName,
            String businessType,
            String storeContactPhone,
            String address,
            boolean pickupEnabled,
            boolean deliveryEnabled
    ) {
        this(
                email,
                password,
                firstName,
                lastName,
                phone,
                storeName,
                businessType,
                storeContactPhone,
                address,
                null,
                null,
                null,
                pickupEnabled,
                deliveryEnabled,
                null,
                null,
                null
        );
    }
}
