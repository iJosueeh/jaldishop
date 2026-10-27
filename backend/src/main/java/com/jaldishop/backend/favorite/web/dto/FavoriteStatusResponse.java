package com.jaldishop.backend.favorite.web.dto;

import java.util.UUID;

public record FavoriteStatusResponse(
        UUID productId,
        boolean isFavorite
) {}
