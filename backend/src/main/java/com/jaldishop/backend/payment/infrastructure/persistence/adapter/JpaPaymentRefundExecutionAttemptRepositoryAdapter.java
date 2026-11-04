package com.jaldishop.backend.payment.infrastructure.persistence.adapter;

import com.jaldishop.backend.payment.domain.model.PaymentRefundExecutionAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentRefundExecutionAttemptRepository;
import com.jaldishop.backend.payment.infrastructure.persistence.mapper.PaymentRefundPersistenceMapper;
import com.jaldishop.backend.payment.infrastructure.persistence.repository.PaymentRefundExecutionAttemptJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class JpaPaymentRefundExecutionAttemptRepositoryAdapter implements PaymentRefundExecutionAttemptRepository {

    private final PaymentRefundExecutionAttemptJpaRepository jpaRepository;

    public JpaPaymentRefundExecutionAttemptRepositoryAdapter(PaymentRefundExecutionAttemptJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PaymentRefundExecutionAttempt save(PaymentRefundExecutionAttempt attempt) {
        var entity = PaymentRefundPersistenceMapper.toEntity(attempt);
        var saved = jpaRepository.save(entity);
        return PaymentRefundPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<PaymentRefundExecutionAttempt> findByRefundIntentIdOrderByAttemptNumberAsc(UUID refundIntentId) {
        return jpaRepository.findByRefundIntentIdOrderByAttemptNumberAsc(refundIntentId).stream()
                .map(PaymentRefundPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public int findNextAttemptNumber(UUID refundIntentId) {
        return jpaRepository.findNextAttemptNumber(refundIntentId);
    }
}
