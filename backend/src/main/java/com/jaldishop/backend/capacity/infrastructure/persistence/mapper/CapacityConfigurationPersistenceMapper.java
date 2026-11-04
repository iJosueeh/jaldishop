package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityConfigurationEntity;
import org.springframework.stereotype.Component;

@Component
public class CapacityConfigurationPersistenceMapper {

    public CapacityConfiguration toDomain(CapacityConfigurationEntity entity) {
        return CapacityConfiguration.reconstitute(
                entity.getId(),
                entity.getStoreId(),
                entity.getDayOfWeek(),
                CapacityPeriod.of(entity.getStartTime(), entity.getEndTime()),
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
                domain.getPeriod() != null ? domain.getPeriod().startTime() : null,
                domain.getPeriod() != null ? domain.getPeriod().endTime() : null,
                domain.getMaxCapacity(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
