package com.jaldishop.backend.checkout.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public interface StoreOperationalSettingsPort {
    OperationalSettings getOperationalSettings(UUID storeId);

    record OperationalSettings(
            UUID storeId,
            boolean requireCustomerDni,
            boolean allowOrderNotes,
            boolean onlinePaymentsEnabled,
            boolean cashOnDeliveryEnabled,
            boolean acceptsPosOnDelivery,
            BigDecimal minDeliveryAmount,
            int estimatedPrepTimeMinutes
    ) {}
}
