package com.jaldishop.backend.favorite.domain.repository;

import com.jaldishop.backend.favorite.domain.model.Favorite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoriteRepository {

    Favorite save(Favorite favorite);

    void delete(UUID userId, UUID productId);

    boolean exists(UUID userId, UUID productId);

    Optional<Favorite> findByUserIdAndProductId(UUID userId, UUID productId);

    List<Favorite> findByUserId(UUID userId);
}
