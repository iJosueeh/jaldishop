package com.jaldishop.backend.catalog.web.dto;

import com.jaldishop.backend.catalog.domain.ProductVariant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ProductVariantResponse(
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
    public static ProductVariantResponse fromDomain(ProductVariant variant) {
        List<VariantAttributeDto> attributeDtos = variant.getAttributes().stream()
                .map(attr -> new VariantAttributeDto(attr.getName(), attr.getValue()))
                .collect(Collectors.toList());

        return new ProductVariantResponse(
                variant.getId(),
                variant.getProductId(),
                variant.getPresentationName(),
                variant.getSku(),
                variant.getPriceAmount(),
                variant.getPriceCurrency(),
                variant.isTracksInventory(),
                variant.getStatus().name(),
                attributeDtos,
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}