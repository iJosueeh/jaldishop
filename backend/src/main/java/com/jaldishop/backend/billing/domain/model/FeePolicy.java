package com.jaldishop.backend.billing.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record FeePolicy(
        String id,
        String name,
        BigDecimal merchantTakeRatePct,
        BigDecimal merchantCashTakeRatePct,
        BigDecimal customerServiceFeeFixed,
        BigDecimal customerServiceFeeThresholdWaive,
        BigDecimal igvRate,
        boolean isActive
) {
    public FeePolicy {
        Objects.requireNonNull(id, "Policy id cannot be null");
        Objects.requireNonNull(name, "Policy name cannot be null");
        merchantTakeRatePct = merchantTakeRatePct != null ? merchantTakeRatePct : new BigDecimal("0.0350");
        merchantCashTakeRatePct = merchantCashTakeRatePct != null ? merchantCashTakeRatePct : new BigDecimal("0.0200");
        customerServiceFeeFixed = customerServiceFeeFixed != null ? customerServiceFeeFixed : new BigDecimal("0.90");
        customerServiceFeeThresholdWaive = customerServiceFeeThresholdWaive != null ? customerServiceFeeThresholdWaive : new BigDecimal("60.00");
        igvRate = igvRate != null ? igvRate : new BigDecimal("0.1800");
    }

    public static FeePolicy defaultPeruCommerce() {
        return new FeePolicy(
                "DEFAULT_PERU_COMMERCE",
                "Política Estándar Comercios Locales Perú",
                new BigDecimal("0.0350"),
                new BigDecimal("0.0200"),
                new BigDecimal("0.90"),
                new BigDecimal("60.00"),
                new BigDecimal("0.1800"),
                true
        );
    }
}
