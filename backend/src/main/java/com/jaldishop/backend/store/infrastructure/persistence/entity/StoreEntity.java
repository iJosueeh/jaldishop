package com.jaldishop.backend.store.infrastructure.persistence.entity;

import com.jaldishop.backend.store.domain.StoreStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "stores")
public class StoreEntity {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, unique = true)
    private UUID ownerUserId;

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

    @Column(name = "tax_rate")
    private BigDecimal taxRate;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @Column(name = "whatsapp_number", length = 30)
    private String whatsappNumber;

    @Column(name = "status", length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private StoreStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "store_category_assignments",
            joinColumns = @JoinColumn(name = "store_id"),
            inverseJoinColumns = @JoinColumn(name = "store_category_id")
    )
    private Set<StoreCategoryEntity> categories = new HashSet<>();

    protected StoreEntity() {
    }

    public StoreEntity(UUID id, UUID ownerUserId, String name, String slug, String description, String contactPhone, String address, String addressReference, BigDecimal latitude, BigDecimal longitude, boolean pickupEnabled, boolean deliveryEnabled, BigDecimal deliveryFeeAmount, String deliveryFeeCurrency, BigDecimal taxRate, StoreStatus status, Instant createdAt, Instant updatedAt) {
        this(id, ownerUserId, name, slug, description, contactPhone, address, addressReference, latitude, longitude, pickupEnabled, deliveryEnabled, deliveryFeeAmount, deliveryFeeCurrency, taxRate, null, null, null, null, null, status, createdAt, updatedAt);
    }

    public StoreEntity(UUID id, UUID ownerUserId, String name, String slug, String description, String contactPhone, String address, String addressReference, BigDecimal latitude, BigDecimal longitude, boolean pickupEnabled, boolean deliveryEnabled, BigDecimal deliveryFeeAmount, String deliveryFeeCurrency, BigDecimal taxRate, String logoUrl, String bannerUrl, String instagramUrl, String facebookUrl, String whatsappNumber, StoreStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
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
        this.taxRate = taxRate;
        this.logoUrl = logoUrl;
        this.bannerUrl = bannerUrl;
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.whatsappNumber = whatsappNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getMerchantUserId() {
        return ownerUserId;
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

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
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

    public Set<StoreCategoryEntity> getCategories() {
        return categories;
    }

    public void setCategories(Set<StoreCategoryEntity> categories) {
        this.categories = categories != null ? categories : new HashSet<>();
    }

}
