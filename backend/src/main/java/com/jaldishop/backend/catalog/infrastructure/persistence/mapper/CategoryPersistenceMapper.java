package com.jaldishop.backend.catalog.infrastructure.persistence.mapper;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.domain.CategoryStatus;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryPersistenceMapper {

    public Category toDomain(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Category(
                entity.getId(),
                entity.getStoreId(),
                entity.getName(),
                entity.getDescription(),
                CategoryStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CategoryEntity toEntity(Category domain) {
        if (domain == null) {
            return null;
        }
        return new CategoryEntity(
                domain.getId(),
                domain.getStoreId(),
                domain.getName(),
                domain.getDescription(),
                domain.getStatus().name(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}