package com.jaldishop.backend.favorite.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.favorite.domain.Favorite;
import com.jaldishop.backend.favorite.domain.FavoriteRepository;
import com.jaldishop.backend.favorite.web.dto.FavoriteStatusResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddFavoriteProductServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ProductRepository productRepository;

    private AddFavoriteProductService service;
    private UUID userId;
    private UUID productId;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        service = new AddFavoriteProductService(favoriteRepository, productRepository);
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        testProduct = new Product(
                productId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Torta de Chocolate",
                "torta-de-chocolate",
                "Deliciosa torta",
                null,
                ProductStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("Agrega el producto a favoritos si existe y aún no estaba marcado")
    void shouldAddFavoriteWhenNotAlreadyMarked() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(favoriteRepository.exists(userId, productId)).thenReturn(false);

        FavoriteStatusResponse response = service.execute(userId, productId);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.isFavorite()).isTrue();
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Es idempotente si el producto ya estaba marcado como favorito")
    void shouldBeIdempotentWhenAlreadyFavorite() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));
        when(favoriteRepository.exists(userId, productId)).thenReturn(true);

        FavoriteStatusResponse response = service.execute(userId, productId);

        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.isFavorite()).isTrue();
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si el producto no existe")
    void shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(userId, productId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }
}
