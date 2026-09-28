package com.jaldishop.backend.capacity.infrastructure.persistence.mapper;

import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityReservationEntity;
import org.springframework.stereotype.Component;

@Component
public class CapacityReservationPersistenceMapper {

    public CapacityReservation toDomain(CapacityReservationEntity entity) {
        return CapacityReservation.reconstitute(
                entity.getId(), entity.getStoreId(), entity.getUserId(), entity.getServiceDate(),
                entity.getStartTime(), entity.getEndTime(), entity.getStatus(),
                entity.getExpiresAt(), entity.getPaymentProtectionExpiresAt(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public CapacityReservationEntity toEntity(CapacityReservation domain) {
        return new CapacityReservationEntity(
                domain.getId(), domain.getStoreId(), domain.getUserId(), domain.getServiceDate(),
                domain.getStartTime(), domain.getEndTime(), domain.getStatus(),
                domain.getExpiresAt(), domain.getPaymentProtectionExpiresAt(),
                domain.getCreatedAt(), domain.getUpdatedAt());
    }
}