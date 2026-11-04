package com.jaldishop.backend.inventory.infrastructure.persistence.mapper;

import com.jaldishop.backend.inventory.domain.model.Inventory;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import org.springframework.stereotype.Component;

@Component
public class InventoryPersistenceMapper {

    public Inventory toDomain(InventoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Inventory(
                entity.getVariantId(),
                entity.getQuantity(),
                entity.getLowStockThreshold(),
                entity.getUpdatedAt()
        );
    }

    public InventoryEntity toEntity(Inventory domain) {
        if (domain == null) {
            return null;
        }
        return new InventoryEntity(
                domain.getVariantId(),
                domain.getQuantity(),
                domain.getLowStockThreshold(),
                domain.getUpdatedAt()
        );
    }
}
