package com.jaldishop.backend.identity.application.command;

import java.math.BigDecimal;

public record UpdatePlatformSettingsCommand(
        Boolean allowMerchantRegistration,
        Boolean requireStoreApproval,
        Boolean maintenanceMode,
        String maintenanceNotice,
        BigDecimal marketplaceCommissionRate,
        BigDecimal customerServiceFeeRate,
        BigDecimal customerServiceFeeCap,
        Long version
) {
    public UpdatePlatformSettingsCommand(
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
