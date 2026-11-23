package com.jaldishop.backend.store.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class StoreSettings {

    private final UUID storeId;
    private final String cancellationPolicy;
    private final String refundPolicy;
    private final boolean requireCustomerDni;
    private final boolean allowOrderNotes;
    private final boolean onlinePaymentsEnabled;
    private final boolean cashOnDeliveryEnabled;
    private final boolean acceptsPosOnDelivery;
    private final BigDecimal minDeliveryAmount;
    private final int estimatedPrepTimeMinutes;
    private final boolean orderSoundAlert;
    private final int soundVolume;
    private final boolean lowStockVisualAlert;
    private final Instant createdAt;
    private final Instant updatedAt;

    public StoreSettings(
            UUID storeId,
            String cancellationPolicy,
            String refundPolicy,
            boolean requireCustomerDni,
            boolean allowOrderNotes,
            boolean onlinePaymentsEnabled,
            boolean cashOnDeliveryEnabled,
            boolean acceptsPosOnDelivery,
            BigDecimal minDeliveryAmount,
            int estimatedPrepTimeMinutes,
            boolean orderSoundAlert,
            int soundVolume,
            boolean lowStockVisualAlert,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.storeId = storeId;
        this.cancellationPolicy = cancellationPolicy;
        this.refundPolicy = refundPolicy;
        this.requireCustomerDni = requireCustomerDni;
        this.allowOrderNotes = allowOrderNotes;
        this.onlinePaymentsEnabled = onlinePaymentsEnabled;
        this.cashOnDeliveryEnabled = cashOnDeliveryEnabled;
        this.acceptsPosOnDelivery = acceptsPosOnDelivery;
        this.minDeliveryAmount = minDeliveryAmount;
        this.estimatedPrepTimeMinutes = estimatedPrepTimeMinutes;
        this.orderSoundAlert = orderSoundAlert;
        this.soundVolume = soundVolume;
        this.lowStockVisualAlert = lowStockVisualAlert;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StoreSettings defaultSettings(UUID storeId) {
        Instant now = Instant.now();
        return new StoreSettings(
                storeId,
                "Puedes solicitar la cancelación gratuita de tu pedido mientras se encuentre en estado Confirmado antes de entrar a preparación.",
                "Conforme a la normativa de INDECOPI, ante fallas o inconsistencias comprobadas se procederá con el reenvío o reembolso del producto dentro de las 24 horas.",
                false,
                true,
                true,
                true,
                false,
                null,
                30,
                true,
                80,
                true,
                now,
                now
        );
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public String getRefundPolicy() {
        return refundPolicy;
    }

    public boolean isRequireCustomerDni() {
        return requireCustomerDni;
    }

    public boolean isAllowOrderNotes() {
        return allowOrderNotes;
    }

    public boolean isOnlinePaymentsEnabled() {
        return onlinePaymentsEnabled;
    }

    public boolean isCashOnDeliveryEnabled() {
        return cashOnDeliveryEnabled;
    }

    public boolean isAcceptsPosOnDelivery() {
        return acceptsPosOnDelivery;
    }

    public BigDecimal getMinDeliveryAmount() {
        return minDeliveryAmount;
    }

    public int getEstimatedPrepTimeMinutes() {
        return estimatedPrepTimeMinutes;
    }

    public boolean isOrderSoundAlert() {
        return orderSoundAlert;
    }

    public int getSoundVolume() {
        return soundVolume;
    }

    public boolean isLowStockVisualAlert() {
        return lowStockVisualAlert;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
