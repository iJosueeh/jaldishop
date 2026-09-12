package com.jaldishop.backend.store.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public class Store {

    private final UUID id;
    private final UUID merchantUserId;
    private String name;
    private final String slug;
    private String description;
    private String contactPhone;
    private String address;
    private String addressReference;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean pickupEnabled;
    private boolean deliveryEnabled;
    private BigDecimal deliveryFeeAmount;
    private String deliveryFeeCurrency;
    private boolean taxApplies;
    private BigDecimal taxRate;
    private StoreStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private Store(UUID id, UUID merchantUserId, String name, String slug, String description, String contactPhone, String address, String addressReference, BigDecimal latitude, BigDecimal longitude, boolean pickupEnabled, boolean deliveryEnabled, BigDecimal deliveryFeeAmount, String deliveryFeeCurrency, boolean taxApplies, BigDecimal taxRate, StoreStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.merchantUserId = merchantUserId;
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase(Locale.ROOT);
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

    public static Store create(
            UUID merchantUserId,
            String name,
            String slug,
            String description,
            String contactPhone,
            String address,
            String addressReference,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean pickupEnabled,
            boolean deliveryEnabled,
            BigDecimal deliveryFeeAmount,
            String deliveryFeeCurrency,
            boolean taxApplies,
            BigDecimal taxRate
    ) {
        if (merchantUserId == null) {
            throw new IllegalArgumentException("El ID del comerciante no puede ser nulo.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio.");
        }

        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("El slug no puede ser nulo ni vacio.");
        }

        if (deliveryEnabled && deliveryFeeAmount != null) {
            if (deliveryFeeAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("La tarifa de entrega no puede ser negativa.");
            }
        }

        if (taxRate != null) {
            if (taxRate.compareTo(BigDecimal.ZERO) <= 0 || taxRate.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("La tasa de impuestos debe ser mayor a 0 y menor o igual a 100.");
            }
        }

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        return new Store(
                id,
                merchantUserId,
                name,
                slug,
                description,
                contactPhone,
                address,
                addressReference,
                latitude,
                longitude,
                pickupEnabled,
                deliveryEnabled,
                deliveryFeeAmount,
                deliveryFeeCurrency,
                taxApplies,
                taxRate,
                StoreStatus.ACTIVE,
                now,
                now
        );
    }

    public static Store reconstitute(
            UUID id,
            UUID merchantUserId,
            String name,
            String slug,
            String description,
            String contactPhone,
            String address,
            String addressReference,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean pickupEnabled,
            boolean deliveryEnabled,
            BigDecimal deliveryFeeAmount,
            String deliveryFeeCurrency,
            boolean taxApplies,
            BigDecimal taxRate,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Store(
                id,
                merchantUserId,
                name,
                slug,
                description,
                contactPhone,
                address,
                addressReference,
                latitude,
                longitude,
                pickupEnabled,
                deliveryEnabled,
                deliveryFeeAmount,
                deliveryFeeCurrency,
                taxApplies,
                taxRate,
                status,
                createdAt,
                updatedAt
        );
    }

    public void update(
            String name,
            String description,
            String contactPhone,
            String address,
            String addressReference,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean pickupEnabled,
            boolean deliveryEnabled,
            BigDecimal deliveryFeeAmount,
            String deliveryFeeCurrency,
            boolean taxApplies,
            BigDecimal taxRate
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio.");
        }

        if (deliveryEnabled && deliveryFeeAmount != null) {
            if (deliveryFeeAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("La tarifa de entrega no puede ser negativa.");
            }
        }

        if (taxRate != null) {
            if (taxRate.compareTo(BigDecimal.ZERO) <= 0 || taxRate.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("La tasa de impuestos debe ser mayor a 0 y menor o igual a 100.");
            }
        }

        this.name = name.trim();
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
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = StoreStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = StoreStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public StoreStatus getStatus() {
        return status;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public boolean isTaxApplies() {
        return taxApplies;
    }

    public String getDeliveryFeeCurrency() {
        return deliveryFeeCurrency;
    }

    public BigDecimal getDeliveryFeeAmount() {
        return deliveryFeeAmount;
    }

    public boolean isDeliveryEnabled() {
        return deliveryEnabled;
    }

    public boolean isPickupEnabled() {
        return pickupEnabled;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public String getAddressReference() {
        return addressReference;
    }

    public String getAddress() {
        return address;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getDescription() {
        return description;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public UUID getMerchantUserId() {
        return merchantUserId;
    }

    public UUID getId() {
        return id;
    }
}
