package com.jaldishop.backend.store.infrastructure.persistence.mapper;

import com.jaldishop.backend.store.domain.StoreCategory;
import com.jaldishop.backend.store.domain.StoreCategoryStatus;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class StoreCategoryPersistenceMapper {

    public StoreCategory toDomain(StoreCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new StoreCategory(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                StoreCategoryStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public StoreCategoryEntity toEntity(StoreCategory domain) {
        if (domain == null) {
            return null;
        }
        return new StoreCategoryEntity(
                domain.getId(),
                domain.getName(),
                domain.getSlug(),
                domain.getDescription(),
                domain.getStatus().name(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
