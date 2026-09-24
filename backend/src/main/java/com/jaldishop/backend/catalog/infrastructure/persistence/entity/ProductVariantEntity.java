package com.jaldishop.backend.catalog.infrastructure.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product_variants")
public class ProductVariantEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "presentation_name", nullable = false, length = 120)
    private String presentationName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Column(name = "price_currency", nullable = false, length = 3)
    private String priceCurrency;

    @Column(name = "tracks_inventory", nullable = false)
    private boolean tracksInventory;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "variant_id", referencedColumnName = "id", insertable = false, updatable = false)
    private List<VariantAttributeEntity> attributes = new ArrayList<>();

    public ProductVariantEntity() {
    }

    public ProductVariantEntity(UUID id, UUID productId, String presentationName, String sku,
                                BigDecimal priceAmount, String priceCurrency, boolean tracksInventory,
                                String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.presentationName = presentationName;
        this.sku = sku;
        this.priceAmount = priceAmount;
        this.priceCurrency = priceCurrency;
        this.tracksInventory = tracksInventory;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getPresentationName() {
        return presentationName;
    }

    public void setPresentationName(String presentationName) {
        this.presentationName = presentationName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public void setPriceAmount(BigDecimal priceAmount) {
        this.priceAmount = priceAmount;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public void setPriceCurrency(String priceCurrency) {
        this.priceCurrency = priceCurrency;
    }

    public boolean isTracksInventory() {
        return tracksInventory;
    }

    public void setTracksInventory(boolean tracksInventory) {
        this.tracksInventory = tracksInventory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<VariantAttributeEntity> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<VariantAttributeEntity> attributes) {
        this.attributes = attributes != null ? attributes : new ArrayList<>();
    }
}