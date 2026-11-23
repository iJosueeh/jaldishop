package com.jaldishop.backend.store.infrastructure.persistence.mapper;

import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreSettingsEntity;
import org.springframework.stereotype.Component;

@Component
public class StoreSettingsPersistenceMapper {

    public StoreSettings toDomain(StoreSettingsEntity entity) {
        if (entity == null) {
            return null;
        }
        return new StoreSettings(
                entity.getStoreId(),
                entity.getCancellationPolicy(),
                entity.getRefundPolicy(),
                entity.isRequireCustomerDni(),
                entity.isAllowOrderNotes(),
                entity.isOnlinePaymentsEnabled(),
                entity.isCashOnDeliveryEnabled(),
                entity.isAcceptsPosOnDelivery(),
                entity.getMinDeliveryAmount(),
                entity.getEstimatedPrepTimeMinutes(),
                entity.isOrderSoundAlert(),
                entity.getSoundVolume(),
                entity.isLowStockVisualAlert(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public StoreSettingsEntity toEntity(StoreSettings domain) {
        if (domain == null) {
            return null;
        }
        StoreSettingsEntity entity = new StoreSettingsEntity();
        entity.setStoreId(domain.getStoreId());
        entity.setCancellationPolicy(domain.getCancellationPolicy());
        entity.setRefundPolicy(domain.getRefundPolicy());
        entity.setRequireCustomerDni(domain.isRequireCustomerDni());
        entity.setAllowOrderNotes(domain.isAllowOrderNotes());
        entity.setOnlinePaymentsEnabled(domain.isOnlinePaymentsEnabled());
        entity.setCashOnDeliveryEnabled(domain.isCashOnDeliveryEnabled());
        entity.setAcceptsPosOnDelivery(domain.isAcceptsPosOnDelivery());
        entity.setMinDeliveryAmount(domain.getMinDeliveryAmount());
        entity.setEstimatedPrepTimeMinutes(domain.getEstimatedPrepTimeMinutes());
        entity.setOrderSoundAlert(domain.isOrderSoundAlert());
        entity.setSoundVolume(domain.getSoundVolume());
        entity.setLowStockVisualAlert(domain.isLowStockVisualAlert());
        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            entity.setUpdatedAt(domain.getUpdatedAt());
        }
        return entity;
    }
}
