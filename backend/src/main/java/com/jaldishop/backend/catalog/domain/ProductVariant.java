package com.jaldishop.backend.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProductVariant {
    private final UUID id;
    private final UUID productId;
    private String presentationName;
    private String sku;
    private BigDecimal priceAmount;
    private String priceCurrency;
    private boolean tracksInventory;
    private VariantStatus status;
    private List<VariantAttribute> attributes;
    private final Instant createdAt;
    private Instant updatedAt;

    public ProductVariant(UUID id, UUID productId, String presentationName, String sku,
                          BigDecimal priceAmount, String priceCurrency, boolean tracksInventory,
                          VariantStatus status, List<VariantAttribute> attributes,
                          Instant createdAt, Instant updatedAt) {
        if (priceAmount == null || priceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        this.id = id;
        this.productId = productId;
        this.presentationName = presentationName;
        this.sku = (sku != null && !sku.isBlank()) ? sku.trim() : null;
        this.priceAmount = priceAmount;
        this.priceCurrency = (priceCurrency != null && !priceCurrency.isBlank()) ? priceCurrency.trim().toUpperCase() : "PEN";
        this.tracksInventory = tracksInventory;
        this.status = status != null ? status : VariantStatus.ACTIVE;
        this.attributes = attributes != null ? new ArrayList<>(attributes) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static ProductVariant create(UUID productId, String presentationName, String sku,
                                        BigDecimal priceAmount, String priceCurrency,
                                        boolean tracksInventory, List<VariantAttribute> attributes) {
        Instant now = Instant.now();
        return new ProductVariant(
                UUID.randomUUID(),
                productId,
                presentationName,
                sku,
                priceAmount,
                priceCurrency,
                tracksInventory,
                VariantStatus.ACTIVE,
                attributes,
                now,
                now
        );
    }

    public void update(String presentationName, String sku, BigDecimal priceAmount,
                       String priceCurrency, boolean tracksInventory, List<VariantAttribute> attributes) {
        if (priceAmount == null || priceAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        }
        this.presentationName = presentationName;
        this.sku = (sku != null && !sku.isBlank()) ? sku.trim() : null;
        this.priceAmount = priceAmount;
        if (priceCurrency != null && !priceCurrency.isBlank()) {
            this.priceCurrency = priceCurrency.trim().toUpperCase();
        }
        this.tracksInventory = tracksInventory;
        if (attributes != null) {
            this.attributes = new ArrayList<>(attributes);
        }
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = VariantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = VariantStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getPresentationName() {
        return presentationName;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public String getPriceCurrency() {
        return priceCurrency;
    }

    public boolean isTracksInventory() {
        return tracksInventory;
    }

    public VariantStatus getStatus() {
        return status;
    }

    public List<VariantAttribute> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}