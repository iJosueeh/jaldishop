package com.jaldishop.backend.favorite.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FavoriteProductResponse(
        UUID productId,
        String productName,
        String productSlug,
        String productImageUrl,
        UUID storeId,
        String storeName,
        String storeSlug,
        BigDecimal minPrice,
        String currency,
        boolean isAvailable,
        Instant addedAt
) {}
