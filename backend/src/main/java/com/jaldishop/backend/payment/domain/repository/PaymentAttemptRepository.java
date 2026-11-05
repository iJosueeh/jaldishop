package com.jaldishop.backend.payment.domain.repository;

import com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository {
    PaymentAttempt save(PaymentAttempt attempt);
    Optional<PaymentAttempt> findById(UUID id);
    Optional<PaymentAttempt> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId);
    Optional<PaymentAttempt> findByProviderAndIdempotencyKey(PaymentProvider provider, String idempotencyKey);
    Optional<PaymentAttempt> findByPreferenceId(String preferenceId);
    Optional<PaymentAttempt> findByProviderOrderId(String providerOrderId);
    List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);
    int findNextAttemptNumber(UUID paymentId);
    Optional<PaymentAttempt> findByIdForUpdate(UUID id);
    List<UUID> findEligibleOrderReconciliationAttemptIds(java.time.Instant cutoffTime, java.time.Instant now, int batchSize);
}
