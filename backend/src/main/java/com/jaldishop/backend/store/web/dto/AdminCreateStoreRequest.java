package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record AdminCreateStoreRequest(
        @NotNull(message = "El ID del comerciante propietario es obligatorio") UUID merchantUserId,
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
        Set<UUID> categoryIds
) {}
