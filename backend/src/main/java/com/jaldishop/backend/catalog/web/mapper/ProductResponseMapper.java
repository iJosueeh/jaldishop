package com.jaldishop.backend.catalog.web.mapper;

import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.catalog.domain.enums.VariantStatus;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.dto.ProductVariantResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
public class ProductResponseMapper {

    private final ProductVariantResponseMapper variantResponseMapper;

    public ProductResponseMapper() {
        this(new ProductVariantResponseMapper());
    }

    public ProductResponseMapper(ProductVariantResponseMapper variantResponseMapper) {
        this.variantResponseMapper = variantResponseMapper;
    }

    public ProductResponse toResponse(Product product) {
        return toResponse(product, null, Collections.emptyMap());
    }

    public ProductResponse toResponse(Product product, List<ProductVariant> variants) {
        return toResponse(product, variants, Collections.emptyMap());
    }

    public ProductResponse toResponse(
            Product product,
            List<ProductVariant> variants,
            Map<UUID, Integer> availableQuantitiesByVariantId
    ) {
        if (product == null) {
            return null;
        }

        Map<UUID, Integer> stockMap = availableQuantitiesByVariantId != null
                ? availableQuantitiesByVariantId
                : Collections.emptyMap();

        List<ProductVariantResponse> variantResponses = variants != null
                ? variants.stream()
                .map(variant -> variantResponseMapper.toResponse(variant, stockMap.get(variant.getId())))
                .toList()
                : Collections.emptyList();

        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        if (variants != null && !variants.isEmpty()) {
            List<BigDecimal> prices = variants.stream()
                    .filter(v -> v.getStatus() == VariantStatus.ACTIVE || variants.size() == 1)
                    .map(ProductVariant::getPriceAmount)
                    .filter(Objects::nonNull)
                    .toList();
            if (!prices.isEmpty()) {
                minPrice = prices.stream().min(BigDecimal::compareTo).orElse(null);
                maxPrice = prices.stream().max(BigDecimal::compareTo).orElse(null);
            }
        }

        return new ProductResponse(
                product.getId(),
                product.getStoreId(),
                product.getCategoryId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus().name(),
                minPrice,
                maxPrice,
                variantResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
