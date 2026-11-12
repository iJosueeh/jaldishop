package com.jaldishop.backend.analytics.web.dto;

import com.jaldishop.backend.analytics.domain.model.ProductSalesRank;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSalesRankResponse(
        UUID variantId,
        UUID productId,
        String productName,
        String variantName,
        long unitsSold,
        BigDecimal totalRevenue
) {
    public static ProductSalesRankResponse from(ProductSalesRank domain) {
        return new ProductSalesRankResponse(
                domain.variantId(),
                domain.productId(),
                domain.productName(),
                domain.variantName(),
                domain.unitsSold(),
                domain.totalRevenue()
        );
    }
}
