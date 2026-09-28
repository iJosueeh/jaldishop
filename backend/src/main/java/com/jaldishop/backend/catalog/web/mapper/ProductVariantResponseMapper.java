package com.jaldishop.backend.catalog.web.mapper;

import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.web.dto.ProductVariantResponse;
import com.jaldishop.backend.catalog.web.dto.VariantAttributeDto;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductVariantResponseMapper {

    public ProductVariantResponse toResponse(ProductVariant variant) {
        if (variant == null) {
            return null;
        }

        List<VariantAttributeDto> attributeDtos = variant.getAttributes() != null
                ? variant.getAttributes().stream()
                .map(attr -> new VariantAttributeDto(attr.getName(), attr.getValue()))
                .collect(Collectors.toList())
                : Collections.emptyList();

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
