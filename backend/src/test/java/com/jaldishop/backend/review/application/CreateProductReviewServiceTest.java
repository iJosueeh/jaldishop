package com.jaldishop.backend.review.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
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
class CreateProductReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private CreateProductReviewService service;

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
                "Jarrón de Cerámica Artesanal",
                "jarron-ceramica",
                "Hermoso jarrón hecho a mano",
                "https://images.example.com/jarron.jpg",
                ProductStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Crea exitosamente una reseña para un producto con compra verificada")
    void shouldCreateReviewSuccessfully() {
        CreateReviewRequest request = new CreateReviewRequest(5, "Excelente acabado y resistencia.");

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(orderRepository.hasCompletedOrderWithProduct(userId, productId)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(mock(Store.class)));

        CustomerReviewResponse response = service.execute(userId, productId, request);

        assertThat(response).isNotNull();
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Excelente acabado y resistencia.");
        assertThat(response.status()).isEqualTo("PUBLISHED");

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Lanza ConflictException si el usuario ya envió una reseña para el producto")
    void shouldThrowConflictWhenReviewAlreadyExists() {
        CreateReviewRequest request = new CreateReviewRequest(4, "Buena calidad.");

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(userId, productId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Ya has enviado una reseña");

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Lanza BusinessRuleException si el usuario no tiene pedido completado con el producto")
    void shouldThrowBusinessRuleExceptionWhenNoCompletedOrder() {
        CreateReviewRequest request = new CreateReviewRequest(4, "Buena calidad.");

        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
        when(reviewRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false);
        when(orderRepository.hasCompletedOrderWithProduct(userId, productId)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(userId, productId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pedido completado");

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el producto no existe")
    void shouldThrowNotFoundWhenProductDoesNotExist() {
        CreateReviewRequest request = new CreateReviewRequest(5, "Excelente");

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(userId, productId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
