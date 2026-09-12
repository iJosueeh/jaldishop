package com.jaldishop.backend.store.infrastructure.persistence.mapper;

import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.stereotype.Component;

@Component
public class StorePersistenceMapper {

    public Store toDomain(StoreEntity entity) {
        return Store.reconstitute(
                entity.getId(),
                entity.getMerchantUserId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getContactPhone(),
                entity.getAddress(),
                entity.getAddressReference(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.isPickupEnabled(),
                entity.isDeliveryEnabled(),
                entity.getDeliveryFeeAmount(),
                entity.getDeliveryFeeCurrency(),
                entity.isTaxApplies(),
                entity.getTaxRate(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public StoreEntity toEntity(Store domain) {
        return new StoreEntity(
                domain.getId(),
                domain.getMerchantUserId(),
                domain.getName(),
                domain.getSlug(),
                domain.getDescription(),
                domain.getContactPhone(),
                domain.getAddress(),
                domain.getAddressReference(),
                domain.getLatitude(),
                domain.getLongitude(),
                domain.isPickupEnabled(),
                domain.isDeliveryEnabled(),
                domain.getDeliveryFeeAmount(),
                domain.getDeliveryFeeCurrency(),
                domain.isTaxApplies(),
                domain.getTaxRate(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

}
