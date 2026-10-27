package com.jaldishop.backend.review.web.dto;

import java.util.List;
import java.util.UUID;

public record ProductReviewsSummaryResponse(
        UUID productId,
        double averageRating,
        int totalReviews,
        List<PublicReviewResponse> reviews
) {}
