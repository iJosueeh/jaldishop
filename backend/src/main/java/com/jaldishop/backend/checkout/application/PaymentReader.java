package com.jaldishop.backend.checkout.application;

import java.util.Optional;
import java.util.UUID;

public interface PaymentReader {

    Optional<PaymentResume> findById(UUID paymentId);

}