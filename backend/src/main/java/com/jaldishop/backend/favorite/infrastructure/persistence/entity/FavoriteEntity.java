package com.jaldishop.backend.favorite.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "favorites")
public class FavoriteEntity {

    @EmbeddedId
    private FavoriteId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public FavoriteEntity() {}

    public FavoriteEntity(UUID userId, UUID productId, Instant createdAt) {
        this.id = new FavoriteId(userId, productId);
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public FavoriteId getId() {
        return id;
    }

    public void setId(FavoriteId id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getUserId() {
        return id != null ? id.getUserId() : null;
    }

    public UUID getProductId() {
        return id != null ? id.getProductId() : null;
    }
}
