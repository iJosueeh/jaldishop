package com.jaldishop.backend.review.domain;

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
}
