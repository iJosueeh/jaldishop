package com.jaldishop.backend.payment.domain.repository;

import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;

import java.util.Optional;
import java.util.UUID;

public interface PaymentIncidenceRepository {
    Optional<PaymentIncidence> findLatestByPaymentIdAndProcessType(UUID paymentId, IncidenceProcessType processType);
    PaymentIncidence save(PaymentIncidence incidence);
    Optional<PaymentIncidence> findById(UUID id);
    Optional<PaymentIncidence> findActiveByPaymentIdAndProcessType(UUID paymentId, IncidenceProcessType processType);
    Optional<PaymentIncidence> findActiveByPaymentIdAndProcessTypeForUpdate(UUID paymentId, IncidenceProcessType processType);
}
