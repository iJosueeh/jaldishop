package com.jaldishop.backend.checkout.infrastructure.persistence.adapter;

import com.jaldishop.backend.checkout.application.PaymentReader;
import com.jaldishop.backend.checkout.application.PaymentResume;
import com.jaldishop.backend.checkout.infrastructure.persistence.entity.PaymentEntity;
import com.jaldishop.backend.checkout.infrastructure.persistence.repository.PaymentJpaRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaPaymentReader implements PaymentReader {

    private final PaymentJpaRepository jpaRepository;

    public JpaPaymentReader(PaymentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<PaymentResume> findById(UUID paymentId) {
        return jpaRepository.findById(paymentId).map(this::toResume);
    }

    private PaymentResume toResume(PaymentEntity entity) {
        return new PaymentResume(
                entity.getId(),
                entity.getCapacityReservationId(),
                entity.getStatus(),
                entity.getApprovedAt(),
                entity.getAmount(),
                entity.getCurrency()
        );
    }

}