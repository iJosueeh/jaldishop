package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.model.StoreSettings;

import java.math.BigDecimal;

public record PublicStoreSettingsResponse(
        String cancellationPolicy,
        String refundPolicy,
        boolean requireCustomerDni,
        boolean allowOrderNotes,
        boolean onlinePaymentsEnabled,
        boolean cashOnDeliveryEnabled,
        boolean acceptsPosOnDelivery,
        BigDecimal minDeliveryAmount,
        int estimatedPrepTimeMinutes
) {
    public static PublicStoreSettingsResponse fromDomain(StoreSettings settings) {
        return new PublicStoreSettingsResponse(
                settings.getCancellationPolicy(),
                settings.getRefundPolicy(),
                settings.isRequireCustomerDni(),
                settings.isAllowOrderNotes(),
                settings.isOnlinePaymentsEnabled(),
                settings.isCashOnDeliveryEnabled(),
                settings.isAcceptsPosOnDelivery(),
                settings.getMinDeliveryAmount(),
                settings.getEstimatedPrepTimeMinutes()
        );
    }
}
