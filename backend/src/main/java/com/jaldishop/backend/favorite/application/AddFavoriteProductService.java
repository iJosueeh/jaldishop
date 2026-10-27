package com.jaldishop.backend.favorite.application;

import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.favorite.domain.Favorite;
import com.jaldishop.backend.favorite.domain.FavoriteRepository;
import com.jaldishop.backend.favorite.web.dto.FavoriteStatusResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class AddFavoriteProductService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;

    public AddFavoriteProductService(FavoriteRepository favoriteRepository,
                                     ProductRepository productRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public FavoriteStatusResponse execute(UUID userId, UUID productId) {
        Objects.requireNonNull(userId, "El identificador del cliente es obligatorio.");
        Objects.requireNonNull(productId, "El identificador del producto es obligatorio.");

        // Verificar que el producto exista
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId));

        if (!favoriteRepository.exists(userId, productId)) {
            favoriteRepository.save(Favorite.create(userId, productId));
        }

        return new FavoriteStatusResponse(productId, true);
    }
}
