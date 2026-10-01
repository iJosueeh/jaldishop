package com.jaldishop.backend.store.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class StoreCategory {

    private final UUID id;
    private String name;
    private final String slug;
    private String description;
    private StoreCategoryStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public StoreCategory(UUID id, String name, String slug, String description, StoreCategoryStatus status, Instant createdAt, Instant updatedAt) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacío.");
        }
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("El slug no puede ser nulo ni vacío.");
        }
        this.id = id;
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase(Locale.ROOT);
        this.description = description;
        this.status = status != null ? status : StoreCategoryStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static StoreCategory create(String name, String slug, String description) {
        Instant now = Instant.now();
        return new StoreCategory(
                UUID.randomUUID(),
                name,
                slug,
                description,
                StoreCategoryStatus.ACTIVE,
                now,
                now
        );
    }

    public void update(String name, String description) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacío.");
        }
        this.name = name.trim();
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = StoreCategoryStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = StoreCategoryStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public String getDescription() {
        return description;
    }

    public StoreCategoryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
