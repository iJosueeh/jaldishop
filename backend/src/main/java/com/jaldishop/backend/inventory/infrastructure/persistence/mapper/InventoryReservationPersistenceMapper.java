package com.jaldishop.backend.inventory.infrastructure.persistence.mapper;

import com.jaldishop.backend.inventory.domain.InventoryReservation;
import com.jaldishop.backend.inventory.domain.InventoryReservationStatus;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryReservationEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryReservationPersistenceMapper {

    public InventoryReservation toDomain(InventoryReservationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new InventoryReservation(
                entity.getId(),
                entity.getStoreId(),
                entity.getCapacityReservationId(),
                entity.getVariantId(),
                entity.getQuantity(),
                InventoryReservationStatus.valueOf(entity.getStatus()),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public InventoryReservationEntity toEntity(InventoryReservation domain) {
        if (domain == null) {
            return null;
        }
        return new InventoryReservationEntity(
                domain.getId(),
                domain.getStoreId(),
                domain.getCapacityReservationId(),
                domain.getVariantId(),
                domain.getQuantity(),
                domain.getStatus().name(),
                domain.getExpiresAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
