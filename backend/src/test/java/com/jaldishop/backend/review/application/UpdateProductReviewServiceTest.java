package com.jaldishop.backend.review.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.review.domain.Review;
import com.jaldishop.backend.review.domain.ReviewRepository;
import com.jaldishop.backend.review.web.dto.CustomerReviewResponse;
import com.jaldishop.backend.review.web.dto.UpdateReviewRequest;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateProductReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private UpdateProductReviewService service;

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
                "Maceta de Arcilla",
                "maceta-arcilla",
                "Maceta para plantas",
                "https://images.example.com/maceta.jpg",
                ProductStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Actualiza exitosamente calificación y comentario de una reseña propia")
    void shouldUpdateReviewSuccessfully() {
        Review review = Review.create(userId, productId, 3, "Calidad aceptable.");
        UpdateReviewRequest request = new UpdateReviewRequest(5, "Actualizo mi reseña: ¡excelente servicio y producto!");

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mock(Store.class)));

        CustomerReviewResponse response = service.execute(userId, productId, request);

        assertThat(response).isNotNull();
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Actualizo mi reseña: ¡excelente servicio y producto!");

        verify(reviewRepository).save(review);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si la reseña previa no existe")
    void shouldThrowNotFoundWhenReviewDoesNotExist() {
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Comentario");

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(reviewRepository.findByUserIdAndProductId(userId, productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(userId, productId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reviewRepository, never()).save(any(Review.class));
    }
}
