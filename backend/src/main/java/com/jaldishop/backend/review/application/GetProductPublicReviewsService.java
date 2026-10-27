package com.jaldishop.backend.review.application;

import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.review.domain.Review;
import com.jaldishop.backend.review.domain.ReviewRepository;
import com.jaldishop.backend.review.domain.ReviewStatus;
import com.jaldishop.backend.review.web.dto.ProductReviewsSummaryResponse;
import com.jaldishop.backend.review.web.dto.PublicReviewResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class GetProductPublicReviewsService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public GetProductPublicReviewsService(ReviewRepository reviewRepository,
                                         ProductRepository productRepository,
                                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProductReviewsSummaryResponse execute(UUID productId) {
        Objects.requireNonNull(productId, "El identificador del producto es obligatorio.");

        if (productRepository.findById(productId).isEmpty()) {
            throw new ResourceNotFoundException("Producto", productId);
        }

        List<Review> reviews = reviewRepository.findByProductIdAndStatus(productId, ReviewStatus.PUBLISHED);

        if (reviews.isEmpty()) {
            return new ProductReviewsSummaryResponse(productId, 0.0, 0, List.of());
        }

        double averageRating = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        averageRating = Math.round(averageRating * 10.0) / 10.0;

        List<PublicReviewResponse> publicReviews = new ArrayList<>();
        for (Review r : reviews) {
            String customerName = "Cliente verificado";
            Optional<User> userOpt = userRepository.findById(r.getUserId());
            if (userOpt.isPresent()) {
                User u = userOpt.get();
                String first = u.getFirstName() != null ? u.getFirstName().trim() : "";
                String last = u.getLastName() != null ? u.getLastName().trim() : "";
                if (!first.isEmpty()) {
                    customerName = first + (last.isEmpty() ? "" : " " + last.charAt(0) + ".");
                }
            }

            publicReviews.add(new PublicReviewResponse(
                    r.getId(),
                    customerName,
                    r.getRating(),
                    r.getComment(),
                    r.getCreatedAt()
            ));
        }

        return new ProductReviewsSummaryResponse(
                productId,
                averageRating,
                reviews.size(),
                publicReviews
        );
    }
}
