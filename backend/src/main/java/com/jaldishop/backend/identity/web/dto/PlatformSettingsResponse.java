package com.jaldishop.backend.identity.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PlatformSettingsResponse(
        boolean allowMerchantRegistration,
        boolean requireStoreApproval,
        boolean maintenanceMode,
        String maintenanceNotice,
        BigDecimal marketplaceCommissionRate,
        BigDecimal customerServiceFeeRate,
        BigDecimal customerServiceFeeCap,
        Long version,
        Instant updatedAt
) {
}
