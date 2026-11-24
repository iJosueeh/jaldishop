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

        @DecimalMin(value = "0.00", message = "customerServiceFeeRate no puede ser menor a 0.00%")
        @DecimalMax(value = "100.00", message = "customerServiceFeeRate no puede exceder 100.00%")
        @Digits(integer = 3, fraction = 2, message = "customerServiceFeeRate debe tener máximo 3 enteros y 2 decimales")
        BigDecimal customerServiceFeeRate,

        @DecimalMin(value = "0.00", message = "customerServiceFeeCap no puede ser negativo")
        @Digits(integer = 10, fraction = 2, message = "customerServiceFeeCap debe tener máximo 10 enteros y 2 decimales")
        BigDecimal customerServiceFeeCap,

        Long version
) {
    public UpdatePlatformSettingsRequest(
            Boolean allowMerchantRegistration,
            Boolean requireStoreApproval,
            Boolean maintenanceMode,
            String maintenanceNotice,
            BigDecimal marketplaceCommissionRate,
            Long version
    ) {
        this(allowMerchantRegistration, requireStoreApproval, maintenanceMode, maintenanceNotice, marketplaceCommissionRate, null, null, version);
    }
}
