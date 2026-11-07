package com.jaldishop.backend.review.application.service;

import com.jaldishop.backend.catalog.domain.enums.ProductStatus;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.review.domain.enums.ReviewStatus;
import com.jaldishop.backend.review.domain.model.Review;
import com.jaldishop.backend.review.domain.repository.ReviewRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteProductReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    private DeleteProductReviewService service;

    private UUID userId;
    private UUID productId;
    private UUID reviewId;
    private Product product;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new DeleteProductReviewService(reviewRepository, productRepository);
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        product = new Product(
                productId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Torta Helada",
                "torta-helada",
                "Descripción",
                null,
                ProductStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        review = new Review(
                reviewId,
                userId,
                productId,
                5,
                "Excelente producto",
                ReviewStatus.PUBLISHED,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Elimina exitosamente la reseña propia del cliente")
    void shouldDeleteOwnReviewSuccessfully() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(review));

        service.execute(userId, productId);

        verify(reviewRepository).delete(reviewId);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el producto no existe")
    void shouldThrowWhenProductNotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(userId, productId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException (404) si la reseña no existe o pertenece a otro usuario")
    void shouldThrowWhenReviewNotFoundOrBelongsToAnotherUser() {
        UUID anotherUser = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(reviewRepository.findByUserIdAndProductId(anotherUser, productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(anotherUser, productId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reviewRepository, never()).delete(any());
    }
}
