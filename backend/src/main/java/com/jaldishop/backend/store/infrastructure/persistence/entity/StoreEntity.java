package com.jaldishop.backend.store.infrastructure.persistence.entity;

import com.jaldishop.backend.store.domain.StoreStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stores")
public class StoreEntity {

    @Id
    private UUID id;

    @Column(name = "merchant_user_id", nullable = false, unique = true)
    private UUID merchantUserId;

    @Column(name = "name", length = 160, nullable = false)
    private String name;

    @Column(name = "slug", length = 180, nullable = false, unique = true)
    private String slug;

    @Column(name = "description", length = 180, columnDefinition = "TEXT")
    private String description;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "address")
    private String address;

    @Column(name = "address_reference")
    private String addressReference;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "pickup_enabled", nullable = false)
    private boolean pickupEnabled;

    @Column(name = "delivery_enabled", nullable = false)
    private boolean deliveryEnabled;

    @Column(name = "delivery_fee_amount", precision = 12, scale = 2)
    private BigDecimal deliveryFeeAmount;

    @Column(name = "delivery_fee_currency", length = 3)
    private String deliveryFeeCurrency;

    @Column(name = "tax_applies", nullable = false)
    private boolean taxApplies;

    @Column(name = "tax_rate")
    private BigDecimal taxRate;

    @Column(name = "status", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private StoreStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StoreEntity() {
    }

    public StoreEntity(UUID id, UUID merchantUserID, String name, String slug, String description, String contactPhone, String address, String addressReference, BigDecimal latitude, BigDecimal longitude, boolean pickupEnabled, boolean deliveryEnabled, BigDecimal deliveryFeeAmount, String deliveryFeeCurrency, boolean taxApplies, BigDecimal taxRate, StoreStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchantUserId = merchantUserID;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.contactPhone = contactPhone;
        this.address = address;
        this.addressReference = addressReference;
        this.latitude = latitude;
        this.longitude = longitude;
        this.pickupEnabled = pickupEnabled;
        this.deliveryEnabled = deliveryEnabled;
        this.deliveryFeeAmount = deliveryFeeAmount;
        this.deliveryFeeCurrency = deliveryFeeCurrency;
        this.taxApplies = taxApplies;
        this.taxRate = taxRate;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMerchantUserId() {
        return merchantUserId;
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

    public String getContactPhone() {
        return contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public String getAddressReference() {
        return addressReference;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public boolean isPickupEnabled() {
        return pickupEnabled;
    }

    public boolean isDeliveryEnabled() {
        return deliveryEnabled;
    }

    public BigDecimal getDeliveryFeeAmount() {
        return deliveryFeeAmount;
    }

    public String getDeliveryFeeCurrency() {
        return deliveryFeeCurrency;
    }

    public boolean isTaxApplies() {
        return taxApplies;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public StoreStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

}
