package com.jaldishop.backend.payment.domain.repository;

import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.model.Payment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(UUID id);
    Optional<Payment> findByIdForUpdate(UUID id);
    Optional<Payment> findByCapacityReservationId(UUID capacityReservationId);
    List<UUID> findEligibleReconciliationPaymentIds(Instant cutoffTime, Instant now, int batchSize);
    List<UUID> findEligibleRefundPaymentIds(int batchSize);
    List<UUID> findEligibleRefundPaymentIds(Instant now, int batchSize);
}
