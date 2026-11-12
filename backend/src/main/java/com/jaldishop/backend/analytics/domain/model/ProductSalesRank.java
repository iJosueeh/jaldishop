package com.jaldishop.backend.analytics.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de rendimiento comercial por producto/variante.
 */
public record ProductSalesRank(
        UUID variantId,
        UUID productId,
        String productName,
        String variantName,
        long unitsSold,
        BigDecimal totalRevenue
) {
    public ProductSalesRank {
        totalRevenue = Objects.requireNonNullElse(totalRevenue, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
