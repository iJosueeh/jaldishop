package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityConfigurationEntity;
import org.springframework.stereotype.Component;

@Component
public class CapacityConfigurationPersistenceMapper {

    public CapacityConfiguration toDomain(CapacityConfigurationEntity entity) {
        return CapacityConfiguration.reconstitute(
                entity.getId(),
                entity.getStoreId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getMaxCapacity(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public CapacityConfigurationEntity toEntity(CapacityConfiguration domain) {
        return new CapacityConfigurationEntity(
                domain.getId(),
                domain.getStoreId(),
                domain.getDayOfWeek(),
                domain.getStartTime(),
                domain.getEndTime(),
                domain.getMaxCapacity(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
