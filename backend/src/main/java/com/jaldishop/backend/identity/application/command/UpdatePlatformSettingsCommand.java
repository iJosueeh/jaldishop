package com.jaldishop.backend.identity.application.command;

import java.math.BigDecimal;

public record UpdatePlatformSettingsCommand(
        Boolean allowMerchantRegistration,
        Boolean requireStoreApproval,
        Boolean maintenanceMode,
        String maintenanceNotice,
        BigDecimal marketplaceCommissionRate,
        Long version
) {
}
