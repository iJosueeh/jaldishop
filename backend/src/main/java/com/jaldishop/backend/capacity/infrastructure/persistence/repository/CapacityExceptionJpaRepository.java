package com.jaldishop.backend.capacity.infrastructure.persistence.repository;

import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CapacityExceptionJpaRepository extends JpaRepository<CapacityExceptionEntity, UUID> {
    List<CapacityExceptionEntity> findByStoreIdOrderByServiceDateDescStartTimeAsc(UUID storeId);
    List<CapacityExceptionEntity> findByStoreIdAndServiceDateOrderByStartTimeAsc(UUID storeId, LocalDate serviceDate);
}
