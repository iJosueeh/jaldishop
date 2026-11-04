package com.jaldishop.backend.payment.domain.repository;

import com.jaldishop.backend.payment.domain.enums.RefundIntentStatus;
import com.jaldishop.backend.payment.domain.model.PaymentRefundIntent;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRefundIntentRepository {
    PaymentRefundIntent save(PaymentRefundIntent intent);
    Optional<PaymentRefundIntent> findById(UUID id);
    Optional<PaymentRefundIntent> findByPaymentId(UUID paymentId);
    Optional<PaymentRefundIntent> findByPaymentIdForUpdate(UUID paymentId);
    Optional<PaymentRefundIntent> findByIdempotencyKey(String idempotencyKey);
}
