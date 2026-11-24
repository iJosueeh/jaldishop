package com.jaldishop.backend.capacity.infrastructure.persistence.repository;

import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityConfigurationEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CapacityConfigurationJpaRepository extends JpaRepository<CapacityConfigurationEntity, UUID> {
    List<CapacityConfigurationEntity> findByStoreIdOrderByDayOfWeekAscStartTimeAsc(UUID
                                                                                           storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CapacityConfigurationEntity c WHERE c.id = :id")
    Optional<CapacityConfigurationEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM CapacityConfigurationEntity c
            WHERE c.storeId = :storeId
              AND c.dayOfWeek = :dayOfWeek
              AND c.startTime IS NOT NULL
              AND c.endTime IS NOT NULL
              AND (:excludeId IS NULL OR c.id <> :excludeId)
              AND (c.startTime < :endTime AND c.endTime > :startTime)
            """)
    boolean existsOverlappingByStoreIdAndDayOfWeek(
            @Param("storeId") UUID storeId,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    @Query(value = """
            SELECT COUNT(*) > 0 FROM capacity_reservations
            WHERE store_id = :storeId
              AND EXTRACT(ISODOW FROM service_date) = :dayOfWeek
              AND status IN ('ACTIVE', 'PAYMENT_PROTECTED', 'COMMITTED')
              AND (
                  (CAST(:startTime AS TIME) IS NULL AND start_time IS NULL)
                  OR (start_time = CAST(:startTime AS TIME) AND end_time = CAST(:endTime AS TIME))
              )
            """, nativeQuery = true)
    boolean hasActiveOrCommittedReservations(
            @Param("storeId") UUID storeId,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Modifying
    @Query(value = "DELETE FROM capacity_configurations WHERE id = :id AND store_id = :storeId", nativeQuery = true)
    void deleteByIdAndStoreId(@Param("id") UUID id, @Param("storeId") UUID storeId);
}
