package com.jaldishop.backend.capacity.infrastructure.persistence.repository;

import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityExceptionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CapacityExceptionJpaRepository extends JpaRepository<CapacityExceptionEntity, UUID> {
    List<CapacityExceptionEntity> findByStoreIdOrderByServiceDateDescStartTimeAsc(UUID storeId);
    List<CapacityExceptionEntity> findByStoreIdAndServiceDateOrderByStartTimeAsc(UUID storeId, LocalDate serviceDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM CapacityExceptionEntity e WHERE e.id = :id")
    Optional<CapacityExceptionEntity> findByIdForUpdate(@Param("id") UUID id);
}
