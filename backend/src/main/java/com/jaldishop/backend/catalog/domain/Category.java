package com.jaldishop.backend.catalog.domain;

import java.time.Instant;
import java.util.UUID;

public class Category {
    private final UUID id;
    private final UUID storeId;
    private String name;
    private String description;
    private CategoryStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Category(UUID id, UUID storeId, String name, String description, CategoryStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.description = description;
        this.status = status != null ? status : CategoryStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    // Factory method para crear una categoría nueva
    public static Category create(UUID storeId, String name, String description) {
        Instant now = Instant.now();
        return new Category(
                UUID.randomUUID(),
                storeId,
                name,
                description,
                CategoryStatus.ACTIVE,
                now,
                now
        );
    }

    // Métodos de comportamiento del dominio
    public void update(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = CategoryStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = CategoryStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public CategoryStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}