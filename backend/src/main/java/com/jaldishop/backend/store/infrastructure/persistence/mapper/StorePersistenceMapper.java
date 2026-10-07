package com.jaldishop.backend.store.infrastructure.persistence.mapper;

import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCategoryEntity;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StorePersistenceMapper {

    public Store toDomain(StoreEntity entity) {
        if (entity == null) {
            return null;
        }
        Set<UUID> categoryIds;
        try {
            categoryIds = (entity.getCategories() != null)
                    ? entity.getCategories().stream().map(StoreCategoryEntity::getId).collect(Collectors.toSet())
                    : Collections.emptySet();
        } catch (Exception ignored) {
            categoryIds = Collections.emptySet();
        }

        return Store.reconstitute(
                entity.getId(),
                entity.getOwnerUserId(),
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
                entity.getTaxRate(),
                entity.getLogoUrl(),
                entity.getBannerUrl(),
                entity.getInstagramUrl(),
                entity.getFacebookUrl(),
                entity.getWhatsappNumber(),
                categoryIds,
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public StoreEntity toEntity(Store domain) {
        if (domain == null) {
            return null;
        }
        return new StoreEntity(
                domain.getId(),
                domain.getOwnerUserId(),
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
                domain.getTaxRate(),
                domain.getLogoUrl(),
                domain.getBannerUrl(),
                domain.getInstagramUrl(),
                domain.getFacebookUrl(),
                domain.getWhatsappNumber(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

}
