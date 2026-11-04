package com.jaldishop.backend.payment.infrastructure.persistence.adapter;

import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.jaldishop.backend.payment.infrastructure.persistence.repository.PaymentJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentPersistenceMapper mapper;

    public JpaPaymentRepositoryAdapter(PaymentJpaRepository jpaRepository, PaymentPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Payment save(Payment payment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByCapacityReservationId(UUID capacityReservationId) {
        return jpaRepository.findByCapacityReservationId(capacityReservationId).map(mapper::toDomain);
    }

    @Override
    public List<UUID> findEligibleReconciliationPaymentIds(Instant cutoffTime, Instant now, int batchSize) {
        int limit = batchSize > 0 ? batchSize : 50;
        return jpaRepository.findEligibleReconciliationPaymentIds(cutoffTime, now, PageRequest.of(0, limit));
    }

    @Override
    public List<UUID> findEligibleRefundPaymentIds(int batchSize) {
        return findEligibleRefundPaymentIds(Instant.now(), batchSize);
    }

    @Override
    public List<UUID> findEligibleRefundPaymentIds(Instant now, int batchSize) {
        int limit = batchSize > 0 ? batchSize : 50;
        Instant effectiveNow = now != null ? now : Instant.now();
        return jpaRepository.findEligibleRefundPaymentIds(effectiveNow, PageRequest.of(0, limit));
    }
}
