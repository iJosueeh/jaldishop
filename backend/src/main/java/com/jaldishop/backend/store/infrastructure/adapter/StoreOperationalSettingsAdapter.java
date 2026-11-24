package com.jaldishop.backend.store.infrastructure.adapter;

import com.jaldishop.backend.checkout.application.port.out.StoreOperationalSettingsPort;
import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.domain.repository.StoreSettingsRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class StoreOperationalSettingsAdapter implements StoreOperationalSettingsPort {

    private final StoreSettingsRepository storeSettingsRepository;

    public StoreOperationalSettingsAdapter(StoreSettingsRepository storeSettingsRepository) {
        this.storeSettingsRepository = storeSettingsRepository;
    }

    @Override
    public OperationalSettings getOperationalSettings(UUID storeId) {
        StoreSettings settings = storeSettingsRepository.findByStoreId(storeId)
                .orElseGet(() -> StoreSettings.defaultSettings(storeId));

        return new OperationalSettings(
                storeId,
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
