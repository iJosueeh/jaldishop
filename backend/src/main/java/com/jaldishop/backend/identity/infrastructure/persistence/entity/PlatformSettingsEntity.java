package com.jaldishop.backend.identity.infrastructure.persistence.entity;

import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "platform_settings")
public class PlatformSettingsEntity {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "allow_merchant_registration", nullable = false)
    private boolean allowMerchantRegistration;

    @Column(name = "require_store_approval", nullable = false)
    private boolean requireStoreApproval;

    @Column(name = "maintenance_mode", nullable = false)
    private boolean maintenanceMode;

    @Column(name = "maintenance_notice", nullable = false, columnDefinition = "TEXT")
    private String maintenanceNotice;

    @Column(name = "marketplace_commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal marketplaceCommissionRate;

    @Column(name = "customer_service_fee_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal customerServiceFeeRate = BigDecimal.ZERO;

    @Column(name = "customer_service_fee_cap", precision = 12, scale = 2)
    private BigDecimal customerServiceFeeCap;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PlatformSettingsEntity() {
    }

    public PlatformSettingsEntity(Integer id, boolean allowMerchantRegistration, boolean requireStoreApproval,
                                  boolean maintenanceMode, String maintenanceNotice,
                                  BigDecimal marketplaceCommissionRate, BigDecimal customerServiceFeeRate,
                                  BigDecimal customerServiceFeeCap, Long version) {
        this.id = id != null ? id : 1;
        this.allowMerchantRegistration = allowMerchantRegistration;
        this.requireStoreApproval = requireStoreApproval;
        this.maintenanceMode = maintenanceMode;
        this.maintenanceNotice = maintenanceNotice;
        this.marketplaceCommissionRate = marketplaceCommissionRate;
        this.customerServiceFeeRate = customerServiceFeeRate != null ? customerServiceFeeRate : BigDecimal.ZERO;
        this.customerServiceFeeCap = customerServiceFeeCap;
        this.version = version != null ? version : 0L;
    }

    public PlatformSettingsEntity(Integer id, boolean allowMerchantRegistration, boolean requireStoreApproval,
                                  boolean maintenanceMode, String maintenanceNotice,
                                  BigDecimal marketplaceCommissionRate, BigDecimal customerServiceFeeRate, Long version) {
        this(id, allowMerchantRegistration, requireStoreApproval, maintenanceMode, maintenanceNotice, marketplaceCommissionRate, customerServiceFeeRate, null, version);
    }

    public PlatformSettingsEntity(Integer id, boolean allowMerchantRegistration, boolean requireStoreApproval,
                                  boolean maintenanceMode, String maintenanceNotice,
                                  BigDecimal marketplaceCommissionRate, Long version) {
        this(id, allowMerchantRegistration, requireStoreApproval, maintenanceMode, maintenanceNotice, marketplaceCommissionRate, BigDecimal.ZERO, null, version);
    }

    public PlatformSettings toDomain() {
        return new PlatformSettings(
                this.id,
                this.allowMerchantRegistration,
                this.requireStoreApproval,
                this.maintenanceMode,
                this.maintenanceNotice,
                this.marketplaceCommissionRate,
                this.customerServiceFeeRate != null ? this.customerServiceFeeRate : BigDecimal.ZERO,
                this.customerServiceFeeCap,
                this.version,
                this.createdAt,
                this.updatedAt
        );
    }

    public static PlatformSettingsEntity fromDomain(PlatformSettings settings) {
        PlatformSettingsEntity entity = new PlatformSettingsEntity(
                settings.id(),
                settings.allowMerchantRegistration(),
                settings.requireStoreApproval(),
                settings.maintenanceMode(),
                settings.maintenanceNotice(),
                settings.marketplaceCommissionRate(),
                settings.customerServiceFeeRate(),
                settings.customerServiceFeeCap(),
                settings.version()
        );
        entity.createdAt = settings.createdAt();
        entity.updatedAt = settings.updatedAt();
        return entity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getId() {
        return id;
    }

    public boolean isAllowMerchantRegistration() {
        return allowMerchantRegistration;
    }

    public void setAllowMerchantRegistration(boolean allowMerchantRegistration) {
        this.allowMerchantRegistration = allowMerchantRegistration;
    }

    public boolean isRequireStoreApproval() {
        return requireStoreApproval;
    }

    public void setRequireStoreApproval(boolean requireStoreApproval) {
        this.requireStoreApproval = requireStoreApproval;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public String getMaintenanceNotice() {
        return maintenanceNotice;
    }

    public void setMaintenanceNotice(String maintenanceNotice) {
        this.maintenanceNotice = maintenanceNotice;
    }

    public BigDecimal getMarketplaceCommissionRate() {
        return marketplaceCommissionRate;
    }

    public void setMarketplaceCommissionRate(BigDecimal marketplaceCommissionRate) {
        this.marketplaceCommissionRate = marketplaceCommissionRate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
