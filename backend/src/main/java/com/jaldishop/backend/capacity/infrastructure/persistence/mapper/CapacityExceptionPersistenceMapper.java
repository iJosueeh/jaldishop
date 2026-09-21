package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityExceptionEntity;
import org.springframework.stereotype.Component;

@Component
public class CapacityExceptionPersistenceMapper {

    public CapacityException toDomain(CapacityExceptionEntity entity) {
        return CapacityException.reconstitute(
                entity.getId(), entity.getStoreId(), entity.getServiceDate(),
                entity.getStartTime(), entity.getEndTime(),
                entity.getExceptionCapacity(), entity.getReason(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public CapacityExceptionEntity toEntity(CapacityException domain) {
        return new CapacityExceptionEntity(
                domain.getId(), domain.getStoreId(), domain.getServiceDate(),
                domain.getStartTime(), domain.getEndTime(),
                domain.getExceptionCapacity(), domain.getReason(),
                domain.getStatus(), domain.getCreatedAt(), domain.getUpdatedAt());
    }
}
