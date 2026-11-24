package com.jaldishop.backend.catalog.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        UUID productId,
        String presentationName,
        String sku,
        BigDecimal priceAmount,
        String priceCurrency,
        boolean tracksInventory,
        Integer availableQuantity,
        String status,
        List<VariantAttributeDto> attributes,
        Instant createdAt,
        Instant updatedAt
) {
    public ProductVariantResponse(
            UUID id,
            UUID productId,
            String presentationName,
            String sku,
            BigDecimal priceAmount,
            String priceCurrency,
            boolean tracksInventory,
            String status,
            List<VariantAttributeDto> attributes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                productId,
                presentationName,
                sku,
                priceAmount,
                priceCurrency,
                tracksInventory,
                null,
                status,
                attributes,
                createdAt,
                updatedAt
        );
    }
}
