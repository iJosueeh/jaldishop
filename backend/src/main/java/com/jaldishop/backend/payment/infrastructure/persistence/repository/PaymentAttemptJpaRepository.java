package com.jaldishop.backend.payment.infrastructure.persistence.repository;

import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentAttemptJpaRepository extends JpaRepository<PaymentAttemptEntity, UUID> {

    Optional<PaymentAttemptEntity> findByProviderAndProviderPaymentId(String provider, String providerPaymentId);

    Optional<PaymentAttemptEntity> findByProviderAndIdempotencyKey(String provider, String idempotencyKey);

    Optional<PaymentAttemptEntity> findByPreferenceId(String preferenceId);

    List<PaymentAttemptEntity> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);

    @Query("SELECT COALESCE(MAX(a.attemptNumber), 0) + 1 FROM PaymentAttemptEntity a WHERE a.paymentId = :paymentId")
    int findNextAttemptNumber(@Param("paymentId") UUID paymentId);

}
