package com.jaldishop.backend.favorite.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Favorite {

    private final UUID userId;
    private final UUID productId;
    private final Instant createdAt;

    public Favorite(UUID userId, UUID productId, Instant createdAt) {
        this.userId = Objects.requireNonNull(userId, "El userId no puede ser nulo.");
        this.productId = Objects.requireNonNull(productId, "El productId no puede ser nulo.");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Favorite create(UUID userId, UUID productId) {
        return new Favorite(userId, productId, Instant.now());
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Favorite favorite = (Favorite) o;
        return Objects.equals(userId, favorite.userId) &&
               Objects.equals(productId, favorite.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, productId);
    }
}
