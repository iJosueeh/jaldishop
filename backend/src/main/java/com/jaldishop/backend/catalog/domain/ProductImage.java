package com.jaldishop.backend.catalog.domain;

import java.time.Instant;
import java.util.UUID;

public class ProductImage {

    private final UUID id;
    private final UUID productId;
    private String imageUrl;
    private int position;
    private boolean primary;
    private final Instant createdAt;

    public ProductImage(UUID id, UUID productId, String imageUrl, int position, boolean primary, Instant createdAt) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la imagen no puede ser nulo.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("El ID del producto no puede ser nulo.");
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("La URL de la imagen no puede estar vacía.");
        }
        if (position < 0) {
            throw new IllegalArgumentException("La posición debe ser mayor o igual a 0.");
        }
        this.id = id;
        this.productId = productId;
        this.imageUrl = imageUrl.trim();
        this.position = position;
        this.primary = primary;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ProductImage create(UUID productId, String imageUrl, int position, boolean primary) {
        return new ProductImage(UUID.randomUUID(), productId, imageUrl, position, primary, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void setPosition(int position) {
        if (position < 0) {
            throw new IllegalArgumentException("La posición debe ser mayor o igual a 0.");
        }
        this.position = position;
    }

    public void setImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("La URL de la imagen no puede estar vacía.");
        }
        this.imageUrl = imageUrl.trim();
    }
}
