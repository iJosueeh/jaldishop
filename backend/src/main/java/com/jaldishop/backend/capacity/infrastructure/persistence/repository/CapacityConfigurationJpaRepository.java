package com.jaldishop.backend.capacity.infrastructure.persistence.repository;

import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityConfigurationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CapacityConfigurationJpaRepository extends JpaRepository<CapacityConfigurationEntity, UUID> {
    List<CapacityConfigurationEntity> findByStoreIdOrderByDayOfWeekAscStartTimeAsc(UUID
                                                                                           storeId);

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
}
