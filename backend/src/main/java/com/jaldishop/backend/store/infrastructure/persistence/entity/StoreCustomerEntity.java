package com.jaldishop.backend.store.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "store_customers")
public class StoreCustomerEntity {

    @EmbeddedId
    private StoreCustomerId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public StoreCustomerEntity() {
    }

    public StoreCustomerEntity(StoreCustomerId id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public StoreCustomerId getId() {
        return id;
    }

    public void setId(StoreCustomerId id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
