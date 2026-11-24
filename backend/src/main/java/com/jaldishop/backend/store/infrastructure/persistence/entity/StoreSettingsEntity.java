package com.jaldishop.backend.store.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "store_settings")
public class StoreSettingsEntity {

    @Id
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "store_id")
    private StoreEntity store;

    @Column(name = "cancellation_policy", columnDefinition = "text", nullable = false)
    private String cancellationPolicy;

    @Column(name = "refund_policy", columnDefinition = "text", nullable = false)
    private String refundPolicy;

    @Column(name = "require_customer_dni", nullable = false)
    private boolean requireCustomerDni;

    @Column(name = "allow_order_notes", nullable = false)
    private boolean allowOrderNotes;

    @Column(name = "online_payments_enabled", nullable = false)
    private boolean onlinePaymentsEnabled;

    @Column(name = "cash_on_delivery_enabled", nullable = false)
    private boolean cashOnDeliveryEnabled;

    @Column(name = "accepts_pos_on_delivery", nullable = false)
    private boolean acceptsPosOnDelivery;

    @Column(name = "min_delivery_amount", precision = 10, scale = 2)
    private BigDecimal minDeliveryAmount;

    @Column(name = "estimated_prep_time_minutes", nullable = false)
    private int estimatedPrepTimeMinutes;

    @Column(name = "order_sound_alert", nullable = false)
    private boolean orderSoundAlert;

    @Column(name = "sound_volume", nullable = false)
    private int soundVolume;

    @Column(name = "low_stock_visual_alert", nullable = false)
    private boolean lowStockVisualAlert;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public StoreSettingsEntity() {
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getStoreId() {
        return storeId;
    }

    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    public StoreEntity getStore() {
        return store;
    }

    public void setStore(StoreEntity store) {
        this.store = store;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public String getRefundPolicy() {
        return refundPolicy;
    }

    public void setRefundPolicy(String refundPolicy) {
        this.refundPolicy = refundPolicy;
    }

    public boolean isRequireCustomerDni() {
        return requireCustomerDni;
    }

    public void setRequireCustomerDni(boolean requireCustomerDni) {
        this.requireCustomerDni = requireCustomerDni;
    }

    public boolean isAllowOrderNotes() {
        return allowOrderNotes;
    }

    public void setAllowOrderNotes(boolean allowOrderNotes) {
        this.allowOrderNotes = allowOrderNotes;
    }

    public boolean isOnlinePaymentsEnabled() {
        return onlinePaymentsEnabled;
    }

    public void setOnlinePaymentsEnabled(boolean onlinePaymentsEnabled) {
        this.onlinePaymentsEnabled = onlinePaymentsEnabled;
    }

    public boolean isCashOnDeliveryEnabled() {
        return cashOnDeliveryEnabled;
    }

    public void setCashOnDeliveryEnabled(boolean cashOnDeliveryEnabled) {
        this.cashOnDeliveryEnabled = cashOnDeliveryEnabled;
    }

    public boolean isAcceptsPosOnDelivery() {
        return acceptsPosOnDelivery;
    }

    public void setAcceptsPosOnDelivery(boolean acceptsPosOnDelivery) {
        this.acceptsPosOnDelivery = acceptsPosOnDelivery;
    }

    public BigDecimal getMinDeliveryAmount() {
        return minDeliveryAmount;
    }

    public void setMinDeliveryAmount(BigDecimal minDeliveryAmount) {
        this.minDeliveryAmount = minDeliveryAmount;
    }

    public int getEstimatedPrepTimeMinutes() {
        return estimatedPrepTimeMinutes;
    }

    public void setEstimatedPrepTimeMinutes(int estimatedPrepTimeMinutes) {
        this.estimatedPrepTimeMinutes = estimatedPrepTimeMinutes;
    }

    public boolean isOrderSoundAlert() {
        return orderSoundAlert;
    }

    public void setOrderSoundAlert(boolean orderSoundAlert) {
        this.orderSoundAlert = orderSoundAlert;
    }

    public int getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(int soundVolume) {
        this.soundVolume = soundVolume;
    }

    public boolean isLowStockVisualAlert() {
        return lowStockVisualAlert;
    }

    public void setLowStockVisualAlert(boolean lowStockVisualAlert) {
        this.lowStockVisualAlert = lowStockVisualAlert;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
