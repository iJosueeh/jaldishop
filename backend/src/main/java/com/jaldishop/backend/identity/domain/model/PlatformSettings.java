package com.jaldishop.backend.identity.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PlatformSettings(
        Integer id,
        boolean allowMerchantRegistration,
        boolean requireStoreApproval,
        boolean maintenanceMode,
        String maintenanceNotice,
        BigDecimal marketplaceCommissionRate,
        BigDecimal customerServiceFeeRate,
        BigDecimal customerServiceFeeCap,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public PlatformSettings(
            Integer id,
            boolean allowMerchantRegistration,
            boolean requireStoreApproval,
            boolean maintenanceMode,
            String maintenanceNotice,
            BigDecimal marketplaceCommissionRate,
            BigDecimal customerServiceFeeRate,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, allowMerchantRegistration, requireStoreApproval, maintenanceMode, maintenanceNotice, marketplaceCommissionRate, customerServiceFeeRate, null, version, createdAt, updatedAt);
    }

    public PlatformSettings(
            Integer id,
            boolean allowMerchantRegistration,
            boolean requireStoreApproval,
            boolean maintenanceMode,
            String maintenanceNotice,
            BigDecimal marketplaceCommissionRate,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, allowMerchantRegistration, requireStoreApproval, maintenanceMode, maintenanceNotice, marketplaceCommissionRate, BigDecimal.ZERO, null, version, createdAt, updatedAt);
    }

    public PlatformSettings(
            Integer id,
            boolean allowMerchantRegistration,
            boolean requireStoreApproval,
            boolean maintenanceMode,
            String maintenanceNotice,
            BigDecimal marketplaceCommissionRate,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, allowMerchantRegistration, requireStoreApproval, maintenanceMode, maintenanceNotice, marketplaceCommissionRate, BigDecimal.ZERO, null, 0L, createdAt, updatedAt);
    }

    public static PlatformSettings defaultSettings() {
        return new PlatformSettings(
                1,
                true,
                false,
                false,
                "La plataforma se encuentra en mantenimiento programado. Volveremos pronto.",
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                null,
                0L,
                Instant.now(),
                Instant.now()
        );
    }
}
