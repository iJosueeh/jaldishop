package com.jaldishop.backend.catalog.infrastructure.persistence.mapper;

import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.VariantAttribute;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.ProductVariantEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.VariantAttributeEntity;
import com.jaldishop.backend.catalog.infrastructure.persistence.entity.VariantAttributeId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProductVariantPersistenceMapper {

    public ProductVariant toDomain(ProductVariantEntity entity) {
        if (entity == null) {
            return null;
        }
        List<VariantAttribute> attributes = entity.getAttributes().stream()
                .map(attr -> new VariantAttribute(attr.getId().getName(), attr.getValue()))
                .collect(Collectors.toList());

        return new ProductVariant(
                entity.getId(),
                entity.getProductId(),
                entity.getPresentationName(),
                entity.getSku(),
                entity.getPriceAmount(),
                entity.getPriceCurrency(),
                entity.isTracksInventory(),
                VariantStatus.valueOf(entity.getStatus()),
                attributes,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductVariantEntity toEntity(ProductVariant domain) {
        if (domain == null) {
            return null;
        }
        ProductVariantEntity entity = new ProductVariantEntity(
                domain.getId(),
                domain.getProductId(),
                domain.getPresentationName(),
                domain.getSku(),
                domain.getPriceAmount(),
                domain.getPriceCurrency(),
                domain.isTracksInventory(),
                domain.getStatus().name(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );

        List<VariantAttributeEntity> attributeEntities = domain.getAttributes().stream()
                .map(attr -> new VariantAttributeEntity(
                        new VariantAttributeId(domain.getId(), attr.getName()),
                        attr.getValue()
                ))
                .collect(Collectors.toList());

        entity.setAttributes(attributeEntities);
        return entity;
    }
}