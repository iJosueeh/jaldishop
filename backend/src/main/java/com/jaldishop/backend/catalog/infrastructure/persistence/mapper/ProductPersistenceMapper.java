package com.jaldishop.backend.catalog.infrastructure.persistence.mapper;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductPersistenceMapper {

    public Product toDomain(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Product(
                entity.getId(),
                entity.getStoreId(),
                entity.getCategoryId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.getImageUrl(),
                ProductStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductEntity toEntity(Product domain) {
        if (domain == null) {
            return null;
        }
        return new ProductEntity(
                domain.getId(),
                domain.getStoreId(),
                domain.getCategoryId(),
                domain.getName(),
                domain.getSlug(),
                domain.getDescription(),
                domain.getImageUrl(),
                domain.getStatus().name(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}