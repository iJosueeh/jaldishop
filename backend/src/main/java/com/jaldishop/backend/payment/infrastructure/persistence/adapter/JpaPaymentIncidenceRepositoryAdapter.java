package com.jaldishop.backend.payment.infrastructure.persistence.adapter;

import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.domain.repository.PaymentIncidenceRepository;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentIncidenceEntity;
import com.jaldishop.backend.payment.infrastructure.persistence.mapper.PaymentIncidencePersistenceMapper;
import com.jaldishop.backend.payment.infrastructure.persistence.repository.PaymentIncidenceJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPaymentIncidenceRepositoryAdapter implements PaymentIncidenceRepository {

    private final PaymentIncidenceJpaRepository jpaRepository;
    private final PaymentIncidencePersistenceMapper mapper;

    public JpaPaymentIncidenceRepositoryAdapter(
            PaymentIncidenceJpaRepository jpaRepository,
            PaymentIncidencePersistenceMapper mapper
    ) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public PaymentIncidence save(PaymentIncidence incidence) {
        PaymentIncidenceEntity entity = mapper.toEntity(incidence);
        PaymentIncidenceEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PaymentIncidence> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentIncidence> findActiveByPaymentIdAndProcessType(UUID paymentId, IncidenceProcessType processType) {
        return jpaRepository.findActiveByPaymentIdAndProcessType(paymentId, processType.name())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PaymentIncidence> findActiveByPaymentIdAndProcessTypeForUpdate(UUID paymentId, IncidenceProcessType processType) {
        return jpaRepository.findActiveByPaymentIdAndProcessTypeForUpdate(paymentId, processType.name())
                .map(mapper::toDomain);
    }
}
