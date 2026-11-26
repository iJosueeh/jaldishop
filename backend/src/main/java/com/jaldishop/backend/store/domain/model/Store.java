package com.jaldishop.backend.store.domain.model;

import com.jaldishop.backend.store.domain.enums.StoreStatus;

import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.util.PhoneUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class Store {

    private final UUID id;
    private final UUID ownerUserId;
    private String name;
    private final String slug;
    private String description;
    private String contactPhone;
    private StoreLocation location;
    private DeliveryConfiguration deliveryConfig;
    private String logoUrl;
    private String bannerUrl;
    private String instagramUrl;
    private String facebookUrl;
    private String whatsappNumber;
    private Set<UUID> categoryIds;
    private StoreStatus status;
    private StoreTaxProfile taxProfile;
    private Instant sunatTermsAcceptedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private Store(UUID id, UUID ownerUserId, String name, String slug, String description,
                  String contactPhone, StoreLocation location, DeliveryConfiguration deliveryConfig,
                  String logoUrl, String bannerUrl, String instagramUrl,
                  String facebookUrl, String whatsappNumber, Set<UUID> categoryIds, StoreStatus status,
                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerUserId = ownerUserId;
        this.name = name.trim();
        this.slug = slug.trim().toLowerCase(Locale.ROOT);
        this.description = description;
        this.contactPhone = contactPhone;
        this.location = location != null ? location : StoreLocation.of(null, null, null, null);
        this.deliveryConfig = deliveryConfig != null ? deliveryConfig : DeliveryConfiguration.of(true, false, null, "PEN", null);
        this.logoUrl = logoUrl;
        this.bannerUrl = bannerUrl;
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.whatsappNumber = whatsappNumber;
        this.categoryIds = categoryIds != null ? new HashSet<>(categoryIds) : new HashSet<>();
        this.status = status;
        this.taxProfile = new StoreTaxProfile(null, com.jaldishop.backend.store.domain.enums.TaxIdType.DNI, null, com.jaldishop.backend.store.domain.enums.TaxRegime.RUS, false);
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
            BigDecimal taxRate
    ) {
        return create(
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
                taxRate,
                null,
                null,
                null,
                null,
                null
        );
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
            BigDecimal taxRate,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber
    ) {
        return create(
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
                taxRate,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                null
        );
    }

    public static Store create(
            UUID ownerUserId,
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
            BigDecimal taxRate,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            Set<UUID> categoryIds
    ) {
        return create(
                ownerUserId,
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
                taxRate,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                categoryIds,
                null,
                null
        );
    }

    public static Store create(
            UUID ownerUserId,
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
            BigDecimal taxRate,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            Set<UUID> categoryIds,
            StoreTaxProfile taxProfile,
            Instant sunatTermsAcceptedAt
    ) {
        if (ownerUserId == null) {
            throw new IllegalArgumentException("El ID del comerciante no puede ser nulo.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio.");
        }

        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("El slug no puede ser nulo ni vacio.");
        }

        StoreTaxProfile resolvedTaxProfile = taxProfile != null
                ? taxProfile
                : new StoreTaxProfile(null, com.jaldishop.backend.store.domain.enums.TaxIdType.DNI, null, com.jaldishop.backend.store.domain.enums.TaxRegime.RUS, false);
        BigDecimal effectiveTaxRate = resolvedTaxProfile.resolveEffectiveTaxRate(taxRate);

        StoreLocation location = StoreLocation.of(address, addressReference, latitude, longitude);
        DeliveryConfiguration deliveryConfig = DeliveryConfiguration.of(
                pickupEnabled, deliveryEnabled, deliveryFeeAmount, deliveryFeeCurrency, effectiveTaxRate
        );

        String normalizedContactPhone = PhoneUtils.normalizePeruPhone(contactPhone);
        String normalizedWhatsapp = PhoneUtils.normalizePeruPhone(whatsappNumber);

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Store store = new Store(
                id,
                ownerUserId,
                name,
                slug,
                description,
                normalizedContactPhone,
                location,
                deliveryConfig,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                normalizedWhatsapp,
                categoryIds,
                StoreStatus.ACTIVE,
                now,
                now
        );
        store.taxProfile = resolvedTaxProfile;
        store.sunatTermsAcceptedAt = sunatTermsAcceptedAt;
        return store;
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
            BigDecimal taxRate,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return reconstitute(
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
                taxRate,
                null,
                null,
                null,
                null,
                null,
                null,
                status,
                createdAt,
                updatedAt
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
            BigDecimal taxRate,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return reconstitute(
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
                taxRate,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                null,
                status,
                createdAt,
                updatedAt
        );
    }

    public static Store reconstitute(
            UUID id,
            UUID ownerUserId,
            String name,
            String slug,
            String description,
            String contactPhone,
            StoreLocation location,
            DeliveryConfiguration deliveryConfig,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            Set<UUID> categoryIds,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Store(
                id,
                ownerUserId,
                name,
                slug,
                description,
                contactPhone,
                location,
                deliveryConfig,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                categoryIds,
                status,
                createdAt,
                updatedAt
        );
    }

    public static Store reconstitute(
            UUID id,
            UUID ownerUserId,
            String name,
            String slug,
            String description,
            String contactPhone,
            StoreLocation location,
            DeliveryConfiguration deliveryConfig,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            Set<UUID> categoryIds,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt,
            StoreTaxProfile taxProfile
    ) {
        return reconstitute(
                id,
                ownerUserId,
                name,
                slug,
                description,
                contactPhone,
                location,
                deliveryConfig,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                categoryIds,
                status,
                createdAt,
                updatedAt,
                taxProfile,
                null
        );
    }

    public static Store reconstitute(
            UUID id,
            UUID ownerUserId,
            String name,
            String slug,
            String description,
            String contactPhone,
            StoreLocation location,
            DeliveryConfiguration deliveryConfig,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            Set<UUID> categoryIds,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt,
            StoreTaxProfile taxProfile,
            Instant sunatTermsAcceptedAt
    ) {
        Store store = new Store(
                id,
                ownerUserId,
                name,
                slug,
                description,
                contactPhone,
                location,
                deliveryConfig,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                categoryIds,
                status,
                createdAt,
                updatedAt
        );
        if (taxProfile != null) {
            store.taxProfile = taxProfile;
        }
        store.sunatTermsAcceptedAt = sunatTermsAcceptedAt;
        return store;
    }

    public static Store reconstitute(
            UUID id,
            UUID ownerUserId,
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
            BigDecimal taxRate,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            Set<UUID> categoryIds,
            StoreStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return reconstitute(
                id,
                ownerUserId,
                name,
                slug,
                description,
                contactPhone,
                StoreLocation.of(address, addressReference, latitude, longitude),
                DeliveryConfiguration.of(pickupEnabled, deliveryEnabled, deliveryFeeAmount, deliveryFeeCurrency, taxRate),
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                categoryIds,
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
            BigDecimal taxRate
    ) {
        update(
                name,
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
                taxRate,
                this.logoUrl,
                this.bannerUrl,
                this.instagramUrl,
                this.facebookUrl,
                this.whatsappNumber,
                this.categoryIds
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
            BigDecimal taxRate,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber
    ) {
        update(
                name,
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
                taxRate,
                logoUrl,
                bannerUrl,
                instagramUrl,
                facebookUrl,
                whatsappNumber,
                this.categoryIds
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
            BigDecimal taxRate,
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber,
            Set<UUID> categoryIds
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo ni vacio.");
        }

        this.location = StoreLocation.of(address, addressReference, latitude, longitude);
        this.deliveryConfig = DeliveryConfiguration.of(
                pickupEnabled, deliveryEnabled, deliveryFeeAmount, deliveryFeeCurrency, taxRate
        );

        this.name = name.trim();
        this.description = description;
        this.contactPhone = PhoneUtils.normalizePeruPhone(contactPhone);
        this.logoUrl = logoUrl;
        this.bannerUrl = bannerUrl;
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.whatsappNumber = PhoneUtils.normalizePeruPhone(whatsappNumber);
        if (categoryIds != null) {
            this.categoryIds = new java.util.HashSet<>(categoryIds);
        }
        this.updatedAt = Instant.now();
    }

    public void assignCategories(Set<UUID> categoryIds) {
        this.categoryIds = categoryIds != null ? new HashSet<>(categoryIds) : new HashSet<>();
        this.updatedAt = Instant.now();
    }

    public Set<UUID> getCategoryIds() {
        return Collections.unmodifiableSet(categoryIds);
    }

    public void updateStorefront(
            String logoUrl,
            String bannerUrl,
            String instagramUrl,
            String facebookUrl,
            String whatsappNumber
    ) {
        this.logoUrl = logoUrl;
        this.bannerUrl = bannerUrl;
        this.instagramUrl = instagramUrl;
        this.facebookUrl = facebookUrl;
        this.whatsappNumber = PhoneUtils.normalizePeruPhone(whatsappNumber);
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (this.status == StoreStatus.ACTIVE) {
            throw new ConflictException("STORE_ALREADY_ACTIVE", "La tienda ya se encuentra activa.");
        }
        this.status = StoreStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        if (this.status == StoreStatus.SUSPENDED) {
            throw new ConflictException("STORE_ALREADY_SUSPENDED", "La tienda ya se encuentra suspendida.");
        }
        this.status = StoreStatus.SUSPENDED;
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

    public StoreLocation getLocation() {
        return location;
    }

    public DeliveryConfiguration getDeliveryConfig() {
        return deliveryConfig;
    }

    public BigDecimal getTaxRate() {
        return deliveryConfig != null ? deliveryConfig.taxRate() : null;
    }

    public String getDeliveryFeeCurrency() {
        return deliveryConfig != null ? deliveryConfig.deliveryFeeCurrency() : null;
    }

    public BigDecimal getDeliveryFeeAmount() {
        return deliveryConfig != null ? deliveryConfig.deliveryFeeAmount() : null;
    }

    public boolean isDeliveryEnabled() {
        return deliveryConfig != null && deliveryConfig.deliveryEnabled();
    }

    public boolean isPickupEnabled() {
        return deliveryConfig != null && deliveryConfig.pickupEnabled();
    }

    public BigDecimal getLongitude() {
        return location != null ? location.longitude() : null;
    }

    public BigDecimal getLatitude() {
        return location != null ? location.latitude() : null;
    }

    public String getAddressReference() {
        return location != null ? location.addressReference() : null;
    }

    public String getAddress() {
        return location != null ? location.address() : null;
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

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getMerchantUserId() {
        return ownerUserId;
    }

    public UUID getId() {
        return id;
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

    public StoreTaxProfile getTaxProfile() {
        return taxProfile;
    }

    public String getLegalBusinessName() {
        return taxProfile != null ? taxProfile.legalBusinessName() : null;
    }

    public com.jaldishop.backend.store.domain.enums.TaxIdType getTaxIdType() {
        return taxProfile != null ? taxProfile.taxIdType() : com.jaldishop.backend.store.domain.enums.TaxIdType.DNI;
    }

    public String getTaxIdNumber() {
        return taxProfile != null ? taxProfile.taxIdNumber() : null;
    }

    public com.jaldishop.backend.store.domain.enums.TaxRegime getTaxRegime() {
        return taxProfile != null ? taxProfile.taxRegime() : com.jaldishop.backend.store.domain.enums.TaxRegime.RUS;
    }

    public boolean isCanIssueInvoices() {
        return taxProfile != null && taxProfile.canIssueInvoices();
    }

    public Instant getSunatTermsAcceptedAt() {
        return sunatTermsAcceptedAt;
    }

    public void updateTaxProfile(StoreTaxProfile taxProfile) {
        updateTaxProfile(taxProfile, null);
    }

    public void updateTaxProfile(StoreTaxProfile taxProfile, BigDecimal requestedTaxRate) {
        StoreTaxProfile resolved = taxProfile != null
                ? taxProfile
                : new StoreTaxProfile(null, com.jaldishop.backend.store.domain.enums.TaxIdType.DNI, null, com.jaldishop.backend.store.domain.enums.TaxRegime.RUS, false);
        BigDecimal effectiveTaxRate = resolved.resolveEffectiveTaxRate(requestedTaxRate);
        this.taxProfile = resolved;
        if (this.deliveryConfig != null) {
            this.deliveryConfig = DeliveryConfiguration.of(
                    this.deliveryConfig.pickupEnabled(),
                    this.deliveryConfig.deliveryEnabled(),
                    this.deliveryConfig.deliveryFeeAmount(),
                    this.deliveryConfig.deliveryFeeCurrency(),
                    effectiveTaxRate
            );
        }
        this.updatedAt = Instant.now();
    }
}
