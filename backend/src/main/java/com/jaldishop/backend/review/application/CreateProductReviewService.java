package com.jaldishop.backend.review.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.ordering.domain.OrderRepository;
import com.jaldishop.backend.review.domain.Review;
import com.jaldishop.backend.review.domain.ReviewRepository;
import com.jaldishop.backend.review.web.dto.CreateReviewRequest;
import com.jaldishop.backend.review.web.dto.CustomerReviewResponse;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreateProductReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    public CreateProductReviewService(ReviewRepository reviewRepository,
                                      OrderRepository orderRepository,
                                      ProductRepository productRepository,
                                      StoreRepository storeRepository) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public CustomerReviewResponse execute(UUID userId, UUID productId, CreateReviewRequest request) {
        Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        Objects.requireNonNull(productId, "El identificador del producto es obligatorio.");
        Objects.requireNonNull(request, "Los datos de la reseña son obligatorios.");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId));

        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ConflictException("Ya has enviado una reseña para este producto. Si deseas cambiarla, actualízala.");
        }

        boolean hasCompletedOrder = orderRepository.hasCompletedOrderWithProduct(userId, productId);
        if (!hasCompletedOrder) {
            throw new BusinessRuleException("Solo puedes calificar productos que hayas adquirido en un pedido completado.");
        }

        Review review = Review.create(userId, productId, request.rating(), request.comment());
        Review saved = reviewRepository.save(review);

        Optional<Store> storeOpt = storeRepository.findById(product.getStoreId());

        return new CustomerReviewResponse(
                saved.getId(),
                product.getId(),
                product.getName(),
                product.getSlug(),
                storeOpt.map(Store::getName).orElse("Comercio Local"),
                storeOpt.map(Store::getSlug).orElse(""),
                product.getImageUrl(),
                saved.getRating(),
                saved.getComment(),
                saved.getStatus().name(),
                saved.getCreatedAt(),
                saved.getUpdatedAt()
        );
    }
}
