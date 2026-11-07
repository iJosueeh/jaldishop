package com.jaldishop.backend.payment.infrastructure.persistence.adapter;

import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;
import com.jaldishop.backend.payment.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
import com.jaldishop.backend.payment.infrastructure.persistence.repository.PaymentAttemptJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPaymentAttemptRepositoryAdapter implements PaymentAttemptRepository {

    private final PaymentAttemptJpaRepository jpaRepository;
    private final PaymentAttemptPersistenceMapper mapper;

    public JpaPaymentAttemptRepositoryAdapter(
            PaymentAttemptJpaRepository jpaRepository,
            PaymentAttemptPersistenceMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentAttempt save(PaymentAttempt attempt) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(attempt)));
    }

    @Override
    public Optional<PaymentAttempt> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAttempt> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId) {
        return jpaRepository.findByProviderAndProviderPaymentId(provider.name(), providerPaymentId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAttempt> findByProviderAndIdempotencyKey(PaymentProvider provider, String idempotencyKey) {
        return jpaRepository.findByProviderAndIdempotencyKey(provider.name(), idempotencyKey)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAttempt> findByPreferenceId(String preferenceId) {
        return jpaRepository.findByPreferenceId(preferenceId).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentAttempt> findByProviderOrderId(String providerOrderId) {
        return jpaRepository.findByProviderOrderId(providerOrderId).map(mapper::toDomain);
    }

    @Override
    public List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId) {
        return jpaRepository.findByPaymentIdOrderByAttemptNumberAsc(paymentId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int findNextAttemptNumber(UUID paymentId) {
        return jpaRepository.findNextAttemptNumber(paymentId);
    }

    @Override
    public Optional<PaymentAttempt> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<UUID> findEligibleOrderReconciliationAttemptIds(java.time.Instant cutoffTime, java.time.Instant now, int batchSize) {
        int limit = Math.max(1, Math.min(batchSize, 100));
        return jpaRepository.findEligibleOrderReconciliationAttemptIds(cutoffTime, now, org.springframework.data.domain.PageRequest.of(0, limit));
    }

}
