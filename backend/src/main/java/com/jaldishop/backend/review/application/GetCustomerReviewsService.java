package com.jaldishop.backend.review.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.review.domain.Review;
import com.jaldishop.backend.review.domain.ReviewRepository;
import com.jaldishop.backend.review.web.dto.CustomerReviewResponse;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GetCustomerReviewsService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public GetCustomerReviewsService(ReviewRepository reviewRepository,
                                     ProductRepository productRepository,
                                     StoreRepository storeRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<CustomerReviewResponse> execute(UUID userId) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");

        List<Review> reviews = reviewRepository.findByUserId(userId);
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }

        List<CustomerReviewResponse> result = new ArrayList<>();
        for (Review review : reviews) {
            Optional<Product> productOpt = productRepository.findById(review.getProductId());
            if (productOpt.isEmpty()) {
                continue;
            }
            Product product = productOpt.get();
            Optional<Store> storeOpt = storeRepository.findById(product.getStoreId());

            result.add(new CustomerReviewResponse(
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
            ));
        }

        return result;
    }
}
