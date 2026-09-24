package com.jaldishop.backend.store.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StoreCustomer {

    private final UUID storeId;
    private final UUID userId;
    private final Instant createdAt;

    public StoreCustomer(UUID storeId, UUID userId, Instant createdAt) {
        this.storeId = Objects.requireNonNull(storeId, "storeId no puede ser nulo");
        this.userId = Objects.requireNonNull(userId, "userId no puede ser nulo");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StoreCustomer create(UUID storeId, UUID userId) {
        return new StoreCustomer(storeId, userId, Instant.now());
    }

    public UUID getStoreId() {
        return storeId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StoreCustomer that = (StoreCustomer) o;
        return Objects.equals(storeId, that.storeId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId, userId);
    }
}
