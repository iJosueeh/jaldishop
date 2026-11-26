package com.jaldishop.backend.review.web.dto;

import java.time.Instant;
import java.util.UUID;

public record PendingReviewProductResponse(
        UUID productId,
        String productName,
        String productSlug,
        String storeName,
        String storeSlug,
        String productImageUrl,
        String lastOrderNumber,
        Instant lastPurchasedAt
) {
}
