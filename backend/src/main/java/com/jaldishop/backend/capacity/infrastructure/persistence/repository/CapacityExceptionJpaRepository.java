package com.jaldishop.backend.capacity.infrastructure.persistence.repository;

import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityExceptionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
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

    @Query(value = """
            SELECT COUNT(*) > 0 FROM capacity_reservations
            WHERE store_id = :storeId
              AND service_date = :serviceDate
              AND status IN ('ACTIVE', 'PAYMENT_PROTECTED', 'COMMITTED')
              AND (
                  (CAST(:startTime AS TIME) IS NULL AND start_time IS NULL)
                  OR (start_time = CAST(:startTime AS TIME) AND end_time = CAST(:endTime AS TIME))
              )
            """, nativeQuery = true)
    boolean hasActiveOrCommittedReservations(
            @Param("storeId") UUID storeId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Modifying
    @Query(value = "DELETE FROM capacity_exceptions WHERE id = :id AND store_id = :storeId", nativeQuery = true)
    void deleteByIdAndStoreId(@Param("id") UUID id, @Param("storeId") UUID storeId);
}
