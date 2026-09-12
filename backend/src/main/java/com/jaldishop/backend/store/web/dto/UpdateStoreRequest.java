package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateStoreRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 160) String name,
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
        boolean taxApplies,
        @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal taxRate
) {}
