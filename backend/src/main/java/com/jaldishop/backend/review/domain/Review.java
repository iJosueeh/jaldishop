package com.jaldishop.backend.review.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Review {

    private final UUID id;
    private final UUID userId;
    private final UUID productId;
    private int rating;
    private String comment;
    private ReviewStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Review(UUID id, UUID userId, UUID productId, int rating, String comment,
                  ReviewStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "El identificador de la reseña es obligatorio.");
        this.userId = Objects.requireNonNull(userId, "El identificador del usuario es obligatorio.");
        this.productId = Objects.requireNonNull(productId, "El identificador del producto es obligatorio.");
        setRating(rating);
        this.comment = comment != null ? comment.trim() : null;
        this.status = Objects.requireNonNull(status, "El estado de la reseña es obligatorio.");
        this.createdAt = Objects.requireNonNull(createdAt, "La fecha de creación es obligatoria.");
        this.updatedAt = Objects.requireNonNull(updatedAt, "La fecha de actualización es obligatoria.");
    }

    public static Review create(UUID userId, UUID productId, int rating, String comment) {
        Instant now = Instant.now();
        return new Review(UUID.randomUUID(), userId, productId, rating, comment, ReviewStatus.PUBLISHED, now, now);
    }

    public void update(int rating, String comment) {
        setRating(rating);
        this.comment = comment != null ? comment.trim() : null;
        this.updatedAt = Instant.now();
    }

    public void hide() {
        this.status = ReviewStatus.HIDDEN;
        this.updatedAt = Instant.now();
    }

    public void publish() {
        this.status = ReviewStatus.PUBLISHED;
        this.updatedAt = Instant.now();
    }

    private void setRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("La calificación debe estar entre 1 y 5 estrellas.");
        }
        this.rating = rating;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getProductId() {
        return productId;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
