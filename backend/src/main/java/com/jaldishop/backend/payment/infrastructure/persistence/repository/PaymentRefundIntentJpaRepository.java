package com.jaldishop.backend.payment.infrastructure.persistence.repository;

import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentRefundIntentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRefundIntentJpaRepository extends JpaRepository<PaymentRefundIntentEntity, UUID> {

    Optional<PaymentRefundIntentEntity> findByPaymentId(UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM PaymentRefundIntentEntity r WHERE r.paymentId = :paymentId")
    Optional<PaymentRefundIntentEntity> findByPaymentIdForUpdate(@Param("paymentId") UUID paymentId);

    Optional<PaymentRefundIntentEntity> findByIdempotencyKey(String idempotencyKey);
}
