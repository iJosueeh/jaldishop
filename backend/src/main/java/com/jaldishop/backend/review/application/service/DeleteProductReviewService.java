package com.jaldishop.backend.review.application.service;

import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.review.domain.model.Review;
import com.jaldishop.backend.review.domain.repository.ReviewRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class DeleteProductReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public DeleteProductReviewService(ReviewRepository reviewRepository,
                                      ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void execute(UUID userId, UUID productId) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        Objects.requireNonNull(productId, "El identificador del producto es obligatorio.");

        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId));

        Review review = reviewRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña", productId));

        reviewRepository.delete(review.getId());
    }
}
