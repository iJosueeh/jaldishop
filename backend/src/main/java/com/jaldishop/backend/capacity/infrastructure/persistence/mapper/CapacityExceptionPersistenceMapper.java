package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.model.CapacityException;
import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityExceptionEntity;
import org.springframework.stereotype.Component;

@Component
public class CapacityExceptionPersistenceMapper {

    public CapacityException toDomain(CapacityExceptionEntity entity) {
        return CapacityException.reconstitute(
                entity.getId(), entity.getStoreId(), entity.getServiceDate(),
                CapacityPeriod.of(entity.getStartTime(), entity.getEndTime()),
                entity.getExceptionCapacity(), entity.getReason(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public CapacityExceptionEntity toEntity(CapacityException domain) {
        return new CapacityExceptionEntity(
                domain.getId(), domain.getStoreId(), domain.getServiceDate(),
                domain.getPeriod() != null ? domain.getPeriod().startTime() : null,
                domain.getPeriod() != null ? domain.getPeriod().endTime() : null,
                domain.getExceptionCapacity(), domain.getReason(),
                domain.getStatus(), domain.getCreatedAt(), domain.getUpdatedAt());
    }
}
