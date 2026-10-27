package com.jaldishop.backend.review.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.ordering.domain.OrderRepository;
import com.jaldishop.backend.review.domain.Review;
import com.jaldishop.backend.review.domain.ReviewRepository;
import com.jaldishop.backend.review.web.dto.CustomerReviewResponse;
import com.jaldishop.backend.review.web.dto.ReviewEligibilityResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckReviewEligibilityService {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public CheckReviewEligibilityService(OrderRepository orderRepository,
                                         ReviewRepository reviewRepository,
                                         ProductRepository productRepository,
                                         StoreRepository storeRepository) {
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse execute(UUID userId, UUID productId) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        Objects.requireNonNull(productId, "El identificador del producto es obligatorio.");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId));

        boolean eligible = orderRepository.hasCompletedOrderWithProduct(userId, productId);
        Optional<Review> existingReviewOpt = reviewRepository.findByUserIdAndProductId(userId, productId);

        CustomerReviewResponse currentReviewDto = null;
        if (existingReviewOpt.isPresent()) {
            Review review = existingReviewOpt.get();
            Optional<Store> storeOpt = storeRepository.findById(product.getStoreId());
            currentReviewDto = new CustomerReviewResponse(
                    review.getId(),
                    product.getId(),
                    product.getName(),
                    product.getSlug(),
                    storeOpt.map(Store::getName).orElse("Comercio Local"),
                    storeOpt.map(Store::getSlug).orElse(""),
                    product.getImageUrl(),
                    review.getRating(),
                    review.getComment(),
                    review.getStatus().name(),
                    review.getCreatedAt(),
                    review.getUpdatedAt()
            );
        }

        return new ReviewEligibilityResponse(
                productId,
                eligible,
                existingReviewOpt.isPresent(),
                currentReviewDto
        );
    }
}
