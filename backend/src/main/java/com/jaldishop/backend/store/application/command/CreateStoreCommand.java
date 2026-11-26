package com.jaldishop.backend.store.application.command;

import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateStoreCommand(
        UUID ownerUserId,
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
        java.util.Set<UUID> categoryIds,
        String legalBusinessName,
        TaxIdType taxIdType,
        String taxIdNumber,
        TaxRegime taxRegime,
        boolean canIssueInvoices,
        Instant sunatTermsAcceptedAt
) {
    public CreateStoreCommand(
            UUID ownerUserId,
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
            java.util.Set<UUID> categoryIds
    ) {
        this(
                ownerUserId, name, slug, description, contactPhone, address, addressReference,
                latitude, longitude, pickupEnabled, deliveryEnabled, deliveryFeeAmount,
                deliveryFeeCurrency, taxRate, logoUrl, bannerUrl, instagramUrl, facebookUrl,
                whatsappNumber, categoryIds, null, null, null, null, false, null
        );
    }
    public UUID merchantUserId() {
        return ownerUserId;
    }
    public CreateStoreCommand(
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
            String whatsappNumber
    ) {
        this(
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
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                null
        );
    }

    public CreateStoreCommand(
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
            BigDecimal taxRate
    ) {
        this(
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
                null
        );
    }
}
