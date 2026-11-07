package com.jaldishop.backend.payment.infrastructure.persistence.repository;

import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentIncidenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentIncidenceJpaRepository extends JpaRepository<PaymentIncidenceEntity, UUID> {

    Optional<PaymentIncidenceEntity> findFirstByPaymentIdAndProcessTypeOrderByCreatedAtDesc(UUID paymentId, String processType);

    @Query("SELECT pi FROM PaymentIncidenceEntity pi WHERE pi.paymentId = :paymentId " +
            "AND pi.processType = :processType AND pi.status IN ('OPEN', 'MANUAL_REVIEW_REQUIRED')")
    Optional<PaymentIncidenceEntity> findActiveByPaymentIdAndProcessType(
            @Param("paymentId") UUID paymentId,
            @Param("processType") String processType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pi FROM PaymentIncidenceEntity pi WHERE pi.paymentId = :paymentId " +
            "AND pi.processType = :processType AND pi.status IN ('OPEN', 'MANUAL_REVIEW_REQUIRED')")
    Optional<PaymentIncidenceEntity> findActiveByPaymentIdAndProcessTypeForUpdate(
            @Param("paymentId") UUID paymentId,
            @Param("processType") String processType
    );
}
