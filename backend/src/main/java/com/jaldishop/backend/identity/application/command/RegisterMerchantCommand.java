package com.jaldishop.backend.identity.application.command;

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
        Set<UUID> categoryIds,
        String legalBusinessName,
        String taxIdType,
        String taxIdNumber,
        String taxRegime,
        boolean canIssueInvoices,
        BigDecimal taxRate,
        boolean sunatTermsAccepted
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
            String addressReference,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean pickupEnabled,
            boolean deliveryEnabled,
            String logoUrl,
            String bannerUrl,
            Set<UUID> categoryIds
    ) {
        this(
                email, password, firstName, lastName, phone, storeName, businessType,
                storeContactPhone, address, addressReference, latitude, longitude,
                pickupEnabled, deliveryEnabled, logoUrl, bannerUrl, categoryIds,
                null, null, null, null, false, null, false
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
            boolean deliveryEnabled,
            String logoUrl,
            String bannerUrl,
            Set<UUID> categoryIds
    ) {
        this(
                email, password, firstName, lastName, phone, storeName, businessType,
                storeContactPhone, address, null, null, null,
                pickupEnabled, deliveryEnabled, logoUrl, bannerUrl, categoryIds,
                null, null, null, null, false, null, false
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
                email, password, firstName, lastName, phone, storeName, businessType,
                storeContactPhone, address, null, null, null,
                pickupEnabled, deliveryEnabled, null, null, null,
                null, null, null, null, false, null, false
        );
    }
}
