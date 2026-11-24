package com.jaldishop.backend.identity.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdatePlatformSettingsRequest(
        @NotNull(message = "allowMerchantRegistration es requerido")
        Boolean allowMerchantRegistration,

        @NotNull(message = "requireStoreApproval es requerido")
        Boolean requireStoreApproval,

        @NotNull(message = "maintenanceMode es requerido")
        Boolean maintenanceMode,

        @Size(max = 1000, message = "maintenanceNotice no puede exceder 1000 caracteres")
        String maintenanceNotice,

        @DecimalMin(value = "0.00", message = "marketplaceCommissionRate no puede ser menor a 0.00%")
        @DecimalMax(value = "100.00", message = "marketplaceCommissionRate no puede exceder 100.00%")
        @Digits(integer = 3, fraction = 2, message = "marketplaceCommissionRate debe tener máximo 3 enteros y 2 decimales")
        BigDecimal marketplaceCommissionRate,

        Long version
) {
}
