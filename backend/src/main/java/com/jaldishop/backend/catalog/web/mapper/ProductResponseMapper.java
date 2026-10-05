package com.jaldishop.backend.catalog.web.mapper;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.catalog.web.dto.ProductResponse;
import com.jaldishop.backend.catalog.web.dto.ProductVariantResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        return toResponse(product, null);
    }

    public ProductResponse toResponse(Product product, List<ProductVariant> variants) {
        if (product == null) {
            return null;
        }

        List<ProductVariantResponse> variantResponses = variants != null
                ? variants.stream().map(variantResponseMapper::toResponse).toList()
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
