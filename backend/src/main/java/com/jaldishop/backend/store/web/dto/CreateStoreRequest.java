package com.jaldishop.backend.store.web.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.jaldishop.backend.shared.validation.ValidPhone;
import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateStoreRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 160) String name,
        @Size(max = 180) String slug,
        String description,
        @ValidPhone String contactPhone,
        String address,
        String addressReference,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        boolean pickupEnabled,
        boolean deliveryEnabled,
        @DecimalMin("0.00") BigDecimal deliveryFeeAmount,
        @Size(min = 3, max = 3) String deliveryFeeCurrency,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal taxRate,
        String logoUrl,
        String bannerUrl,
        String instagramUrl,
        String facebookUrl,
        @ValidPhone String whatsappNumber,
        java.util.Set<java.util.UUID> categoryIds,
        @Size(max = 200) String legalBusinessName,
        TaxIdType taxIdType,
        @Size(max = 11) String taxIdNumber,
        TaxRegime taxRegime,
        Boolean canIssueInvoices,
        Boolean sunatTermsAccepted
) {
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CreateStoreRequest {
    }

    public CreateStoreRequest(
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
            java.util.Set<java.util.UUID> categoryIds
    ) {
        this(
                name, slug, description, contactPhone, address, addressReference,
                latitude, longitude, pickupEnabled, deliveryEnabled, deliveryFeeAmount,
                deliveryFeeCurrency, taxRate, logoUrl, bannerUrl, instagramUrl,
                facebookUrl, whatsappNumber, categoryIds,
                null, null, null, null, false, null
        );
    }
    public CreateStoreRequest(
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

    public CreateStoreRequest(
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
