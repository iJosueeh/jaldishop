package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.model.CapacityPeriod;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityReservationEntity;
import org.springframework.stereotype.Component;

@Component
public class CapacityReservationPersistenceMapper {

    public CapacityReservation toDomain(CapacityReservationEntity entity) {
        return CapacityReservation.reconstitute(
                entity.getId(), entity.getStoreId(), entity.getUserId(), entity.getServiceDate(),
                CapacityPeriod.of(entity.getStartTime(), entity.getEndTime()), entity.getStatus(),
                entity.getExpiresAt(), entity.getPaymentProtectionExpiresAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public CapacityReservationEntity toEntity(CapacityReservation domain) {
        return new CapacityReservationEntity(
                domain.getId(), domain.getStoreId(), domain.getUserId(), domain.getServiceDate(),
                domain.getPeriod() != null ? domain.getPeriod().startTime() : null,
                domain.getPeriod() != null ? domain.getPeriod().endTime() : null,
                domain.getStatus(),
                domain.getExpiresAt(), domain.getPaymentProtectionExpiresAt(),
                domain.getCreatedAt(), domain.getUpdatedAt());
    }
}