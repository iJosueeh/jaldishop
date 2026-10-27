package com.jaldishop.backend.review.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerReviewResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSlug,
        String storeName,
        String storeSlug,
        String imageUrl,
        int rating,
        String comment,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
