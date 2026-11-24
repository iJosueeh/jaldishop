package com.jaldishop.backend.media.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_cleanup_outbox")
public class MediaCleanupOutboxEntity {

    @Id
    private UUID id;

    @Column(name = "public_id", length = 500)
    private String publicId;

    @Column(name = "prefix", length = 500)
    private String prefix;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected MediaCleanupOutboxEntity() {
    }

    public static MediaCleanupOutboxEntity forPublicId(String publicId) {
        MediaCleanupOutboxEntity entity = new MediaCleanupOutboxEntity();
        entity.id = UUID.randomUUID();
        entity.publicId = publicId;
        entity.status = "PENDING";
        entity.attempts = 0;
        entity.createdAt = Instant.now();
        return entity;
    }

    public static MediaCleanupOutboxEntity forPrefix(String prefix) {
        MediaCleanupOutboxEntity entity = new MediaCleanupOutboxEntity();
        entity.id = UUID.randomUUID();
        entity.prefix = prefix;
        entity.status = "PENDING";
        entity.attempts = 0;
        entity.createdAt = Instant.now();
        return entity;
    }

    public UUID getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void markCompleted() {
        this.status = "COMPLETED";
        this.processedAt = Instant.now();
        this.lastError = null;
    }

    public void recordFailure(String errorMessage, int maxAttempts) {
        this.attempts += 1;
        this.lastError = errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage;
        if (this.attempts >= maxAttempts) {
            this.status = "FAILED";
            this.processedAt = Instant.now();
        }
    }
}
