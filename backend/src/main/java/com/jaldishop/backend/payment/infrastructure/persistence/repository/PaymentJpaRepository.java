package com.jaldishop.backend.payment.infrastructure.persistence.repository;

import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByCapacityReservationId(UUID capacityReservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentEntity p WHERE p.id = :id")
    Optional<PaymentEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT p.id FROM PaymentEntity p WHERE p.status = 'APPROVED' " +
            "AND p.refundStatus = 'NOT_REQUIRED' " +
            "AND p.approvedAt IS NOT NULL AND p.approvedAt <= :cutoffTime " +
            "AND NOT EXISTS (SELECT o.id FROM OrderEntity o WHERE o.paymentId = p.id) " +
            "AND NOT EXISTS (SELECT pi.id FROM PaymentIncidenceEntity pi WHERE pi.paymentId = p.id " +
            "AND pi.processType = 'ORDER_CONFIRMATION' " +
            "AND (pi.status = 'MANUAL_REVIEW_REQUIRED' OR pi.status = 'RESOLVED' " +
            "OR (pi.status = 'OPEN' AND pi.nextRetryAt > :now) " +
            "OR (pi.status = 'OPEN' AND pi.attemptsCount >= pi.maxAttempts))) " +
            "ORDER BY p.approvedAt ASC")
    List<UUID> findEligibleReconciliationPaymentIds(
            @Param("cutoffTime") Instant cutoffTime,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("SELECT p.id FROM PaymentEntity p WHERE p.status = 'APPROVED' " +
            "AND (p.refundStatus = 'PENDING' OR p.refundStatus = 'PROCESSING') " +
            "AND NOT EXISTS (SELECT o.id FROM OrderEntity o WHERE o.paymentId = p.id) " +
            "AND NOT EXISTS (SELECT pi.id FROM PaymentIncidenceEntity pi WHERE pi.paymentId = p.id " +
            "AND pi.processType = 'REFUND_EXECUTION' " +
            "AND (pi.status = 'MANUAL_REVIEW_REQUIRED' " +
            "OR (pi.status = 'OPEN' AND pi.nextRetryAt > :now) " +
            "OR (pi.status = 'OPEN' AND pi.attemptsCount >= pi.maxAttempts))) " +
            "ORDER BY p.updatedAt ASC")
    List<UUID> findEligibleRefundPaymentIds(@Param("now") Instant now, Pageable pageable);
}
