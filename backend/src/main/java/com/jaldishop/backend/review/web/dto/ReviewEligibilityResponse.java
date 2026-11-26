package com.jaldishop.backend.review.web.dto;

import java.util.UUID;

public record ReviewEligibilityResponse(
        UUID productId,
        boolean eligible,
        boolean alreadyReviewed,
        CustomerReviewResponse currentReview,
        boolean hasInProgressOrder
) {
    public ReviewEligibilityResponse(
            UUID productId,
            boolean eligible,
            boolean alreadyReviewed,
            CustomerReviewResponse currentReview
    ) {
        this(productId, eligible, alreadyReviewed, currentReview, false);
    }
}
