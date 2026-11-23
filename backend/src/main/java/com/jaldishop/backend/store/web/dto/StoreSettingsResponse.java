package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.model.StoreSettings;

import java.math.BigDecimal;

public record StoreSettingsResponse(
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
        boolean lowStockVisualAlert
) {
    public static StoreSettingsResponse fromDomain(StoreSettings settings) {
        return new StoreSettingsResponse(
                settings.getCancellationPolicy(),
                settings.getRefundPolicy(),
                settings.isRequireCustomerDni(),
                settings.isAllowOrderNotes(),
                settings.isOnlinePaymentsEnabled(),
                settings.isCashOnDeliveryEnabled(),
                settings.isAcceptsPosOnDelivery(),
                settings.getMinDeliveryAmount(),
                settings.getEstimatedPrepTimeMinutes(),
                settings.isOrderSoundAlert(),
                settings.getSoundVolume(),
                settings.isLowStockVisualAlert()
        );
    }
}
