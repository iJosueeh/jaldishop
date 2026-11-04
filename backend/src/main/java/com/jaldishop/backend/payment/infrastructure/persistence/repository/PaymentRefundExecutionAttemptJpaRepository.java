package com.jaldishop.backend.payment.infrastructure.persistence.repository;

import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentRefundExecutionAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PaymentRefundExecutionAttemptJpaRepository extends JpaRepository<PaymentRefundExecutionAttemptEntity, UUID> {

    List<PaymentRefundExecutionAttemptEntity> findByRefundIntentIdOrderByAttemptNumberAsc(UUID refundIntentId);

    @Query("SELECT COALESCE(MAX(a.attemptNumber), 0) + 1 FROM PaymentRefundExecutionAttemptEntity a WHERE a.refundIntentId = :refundIntentId")
    int findNextAttemptNumber(@Param("refundIntentId") UUID refundIntentId);
}
