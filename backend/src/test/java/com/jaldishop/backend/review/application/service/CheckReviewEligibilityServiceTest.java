package com.jaldishop.backend.review.application.service;

import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.catalog.domain.enums.ProductStatus;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.review.domain.model.Review;
import com.jaldishop.backend.review.domain.repository.ReviewRepository;
import com.jaldishop.backend.review.web.dto.ReviewEligibilityResponse;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckReviewEligibilityServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private CheckReviewEligibilityService service;

    private UUID userId;
    private UUID productId;
    private UUID storeId;
    private Product mockProduct;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        mockProduct = new Product(
                productId,
                storeId,
                UUID.randomUUID(),
                "Cojín bordado a mano",
                "cojin-bordado",
                "Cojín con tela artesanal",
                "https://images.example.com/cojin.jpg",
                ProductStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Retorna eligible=true y alreadyReviewed=false si tiene compra completada sin reseñar")
    void shouldReturnEligibleWhenHasCompletedOrderAndNoReview() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(orderRepository.hasCompletedOrderWithProduct(userId, productId)).thenReturn(true);
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        ReviewEligibilityResponse response = service.execute(userId, productId);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.eligible()).isTrue();
        assertThat(response.alreadyReviewed()).isFalse();
        assertThat(response.currentReview()).isNull();
    }

    @Test
    @DisplayName("Retorna alreadyReviewed=true y los datos de la reseña si ya la registró")
    void shouldReturnAlreadyReviewedWithCurrentReview() {
        Review review = Review.create(userId, productId, 4, "Muy bonito tejido.");

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(orderRepository.hasCompletedOrderWithProduct(userId, productId)).thenReturn(true);
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(review));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mock(Store.class)));

        ReviewEligibilityResponse response = service.execute(userId, productId);

        assertThat(response).isNotNull();
        assertThat(response.eligible()).isTrue();
        assertThat(response.alreadyReviewed()).isTrue();
        assertThat(response.currentReview()).isNotNull();
        assertThat(response.currentReview().rating()).isEqualTo(4);
        assertThat(response.currentReview().comment()).isEqualTo("Muy bonito tejido.");
    }
}
