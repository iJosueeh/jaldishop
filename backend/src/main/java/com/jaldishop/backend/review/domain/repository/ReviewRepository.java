package com.jaldishop.backend.review.domain.repository;

import com.jaldishop.backend.review.domain.model.Review;
import com.jaldishop.backend.review.domain.enums.ReviewStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    Review save(Review review);
    Optional<Review> findById(UUID id);
    Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId);
    List<Review> findByUserId(UUID userId);
    List<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status);
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    void delete(UUID id);
}
