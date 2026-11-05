package com.jaldishop.backend.payment.infrastructure.persistence.repository;

import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentAttemptEntity;
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
public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptEntity, UUID> {

    Optional<PaymentAttemptEntity> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

    Optional<PaymentAttemptEntity> findByProviderAndIdempotencyKey(String provider, String idempotencyKey);

    Optional<PaymentAttemptEntity> findByPreferenceId(String preferenceId);

    Optional<PaymentAttemptEntity> findByProviderOrderId(String providerOrderId);

    List<PaymentAttemptEntity> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);

    @Query("SELECT COALESCE(MAX(a.attemptNumber), 0) + 1 FROM PaymentAttemptEntity a WHERE a.paymentId = :paymentId")
    int findNextAttemptNumber(@Param("paymentId") UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PaymentAttemptEntity a WHERE a.id = :id")
    Optional<PaymentAttemptEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT pa.id FROM PaymentAttemptEntity pa, PaymentEntity p " +
            "WHERE pa.paymentId = p.id " +
            "AND p.status != 'APPROVED' " +
            "AND pa.checkoutMode = 'ORDERS' " +
            "AND pa.providerOrderId IS NOT NULL " +
            "AND pa.status IN ('CREATED', 'PROCESSING') " +
            "AND pa.createdAt <= :cutoffTime " +
            "AND (pa.reconciliationLeaseExpiresAt IS NULL OR pa.reconciliationLeaseExpiresAt < :now) " +
            "ORDER BY pa.createdAt ASC")
    List<UUID> findEligibleOrderReconciliationAttemptIds(
            @Param("cutoffTime") Instant cutoffTime,
            @Param("now") Instant now,
            Pageable pageable
    );

}
