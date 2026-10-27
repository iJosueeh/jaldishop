package com.jaldishop.backend.review.web.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicReviewResponse(
        UUID id,
        String customerName,
        int rating,
        String comment,
        Instant createdAt
) {}
