package com.jaldishop.backend.payment.infrastructure.persistence.adapter;

import com.jaldishop.backend.payment.domain.model.PaymentRefundIntent;
import com.jaldishop.backend.payment.domain.repository.PaymentRefundIntentRepository;
import com.jaldishop.backend.payment.infrastructure.persistence.mapper.PaymentRefundPersistenceMapper;
import com.jaldishop.backend.payment.infrastructure.persistence.repository.PaymentRefundIntentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPaymentRefundIntentRepositoryAdapter implements PaymentRefundIntentRepository {

    private final PaymentRefundIntentJpaRepository jpaRepository;

    public JpaPaymentRefundIntentRepositoryAdapter(PaymentRefundIntentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentRefundIntent save(PaymentRefundIntent intent) {
        var entity = PaymentRefundPersistenceMapper.toEntity(intent);
        var saved = jpaRepository.save(entity);
        return PaymentRefundPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<PaymentRefundIntent> findById(UUID id) {
        return jpaRepository.findById(id).map(PaymentRefundPersistenceMapper::toDomain);
    }

    @Override
    public Optional<PaymentRefundIntent> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentId(paymentId).map(PaymentRefundPersistenceMapper::toDomain);
    }

    @Override
    public Optional<PaymentRefundIntent> findByPaymentIdForUpdate(UUID paymentId) {
        return jpaRepository.findByPaymentIdForUpdate(paymentId).map(PaymentRefundPersistenceMapper::toDomain);
    }

    @Override
    public Optional<PaymentRefundIntent> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(PaymentRefundPersistenceMapper::toDomain);
    }
}
