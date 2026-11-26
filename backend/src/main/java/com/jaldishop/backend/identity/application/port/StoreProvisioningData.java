package com.jaldishop.backend.identity.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Contrato de aprovisionamiento de tienda usado en el registro de comerciantes.
 * Mantiene la frontera hexagonal entre identity/application y store/domain
 * transportando tipos primitivos/simples para los datos fiscales SUNAT.
 */
public record StoreProvisioningData(
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
        Set<UUID> categoryIds,
        String legalBusinessName,
        String taxIdType,
        String taxIdNumber,
        String taxRegime,
        boolean canIssueInvoices,
        Instant sunatTermsAcceptedAt
) {
    public StoreProvisioningData(
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
            Set<UUID> categoryIds
    ) {
        this(
                ownerUserId, name, slug, description, contactPhone, address, addressReference,
                latitude, longitude, pickupEnabled, deliveryEnabled, deliveryFeeAmount,
                deliveryFeeCurrency, taxRate, logoUrl, bannerUrl, instagramUrl, facebookUrl,
                whatsappNumber, categoryIds, null, null, null, null, false, null
        );
    }
}
