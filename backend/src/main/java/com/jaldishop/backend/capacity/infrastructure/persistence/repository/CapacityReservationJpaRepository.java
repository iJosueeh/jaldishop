package com.jaldishop.backend.capacity.infrastructure.persistence.repository;

import com.jaldishop.backend.capacity.infrastructure.persistence.entity.CapacityReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Repository
public interface CapacityReservationJpaRepository extends JpaRepository<CapacityReservationEntity, UUID> {

    @Query("""
            SELECT COUNT(r) FROM CapacityReservationEntity r
            WHERE r.storeId = :storeId
              AND r.serviceDate = :serviceDate
              AND r.startTime = :startTime
              AND r.endTime = :endTime
              AND (
                   (r.status = 'ACTIVE' AND r.expiresAt > :now)
                OR (r.status = 'PAYMENT_PROTECTED' AND r.paymentProtectionExpiresAt IS NOT NULL AND r.paymentProtectionExpiresAt > :now)
              )
            """)
    long countReserved(
            @Param("storeId") UUID storeId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("now") Instant now);

    @Query("""
            SELECT COUNT(r) FROM CapacityReservationEntity r
            WHERE r.storeId = :storeId
              AND r.serviceDate = :serviceDate
              AND r.startTime = :startTime
              AND r.endTime = :endTime
              AND r.status = 'COMMITTED'
            """)
    long countCommitted(
            @Param("storeId") UUID storeId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM CapacityReservationEntity r
            WHERE r.storeId = :storeId
              AND r.userId = :userId
              AND r.serviceDate = :serviceDate
              AND r.startTime = :startTime
              AND r.endTime = :endTime
              AND (
                   (r.status = 'ACTIVE' AND r.expiresAt > :now)
                OR (r.status = 'PAYMENT_PROTECTED' AND r.paymentProtectionExpiresAt IS NOT NULL AND r.paymentProtectionExpiresAt > :now)
              )
            """)
    boolean existsReservedByUserAndWindow(
            @Param("userId") UUID userId,
            @Param("storeId") UUID storeId,
            @Param("serviceDate") LocalDate serviceDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("now") Instant now);
}