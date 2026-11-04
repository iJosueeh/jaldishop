package com.jaldishop.backend.payment.domain.repository;

import com.jaldishop.backend.payment.domain.model.PaymentRefundExecutionAttempt;

import java.util.List;
import java.util.UUID;

public interface PaymentRefundExecutionAttemptRepository {
    PaymentRefundExecutionAttempt save(PaymentRefundExecutionAttempt attempt);
    List<PaymentRefundExecutionAttempt> findByRefundIntentIdOrderByAttemptNumberAsc(UUID refundIntentId);
    int findNextAttemptNumber(UUID refundIntentId);
}
