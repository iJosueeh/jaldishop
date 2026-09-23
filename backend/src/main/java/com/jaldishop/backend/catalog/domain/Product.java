package com.jaldishop.backend.catalog.domain;

import java.time.Instant;
import java.util.UUID;

public class Product {
    private final UUID id;
    private final UUID storeId;
    private UUID categoryId;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private ProductStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public Product(UUID id, UUID storeId, UUID categoryId, String name, String slug,
                   String description, String imageUrl, ProductStatus status,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.storeId = storeId;
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.imageUrl = imageUrl;
        this.status = status != null ? status : ProductStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Product create(UUID storeId, UUID categoryId, String name, String slug,
                                 String description, String imageUrl) {
        Instant now = Instant.now();
        return new Product(
                UUID.randomUUID(),
                storeId,
                categoryId,
                name,
                slug,
                description,
                imageUrl,
                ProductStatus.ACTIVE,
                now,
                now
        );
    }

    public void update(UUID categoryId, String name, String slug, String description, String imageUrl) {
        this.categoryId = categoryId;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.imageUrl = imageUrl;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public UUID getCategoryId() {
        return categoryId;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}