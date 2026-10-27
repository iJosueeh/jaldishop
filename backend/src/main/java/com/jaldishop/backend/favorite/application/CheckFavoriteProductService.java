package com.jaldishop.backend.favorite.application;

import com.jaldishop.backend.favorite.domain.FavoriteRepository;
import com.jaldishop.backend.favorite.web.dto.FavoriteStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class CheckFavoriteProductService {

    private final FavoriteRepository favoriteRepository;

    public CheckFavoriteProductService(FavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional(readOnly = true)
    public FavoriteStatusResponse execute(UUID userId, UUID productId) {
        Objects.requireNonNull(userId, "El identificador del cliente es obligatorio.");
        Objects.requireNonNull(productId, "El identificador del producto es obligatorio.");

        boolean isFavorite = favoriteRepository.exists(userId, productId);
        return new FavoriteStatusResponse(productId, isFavorite);
    }
}
