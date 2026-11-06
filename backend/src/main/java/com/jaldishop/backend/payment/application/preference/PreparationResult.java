package com.jaldishop.backend.payment.application.preference;

import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;

import java.time.Instant;
import java.util.List;

public record PreparationResult(
        Payment payment,
        PaymentAttempt attempt,
        boolean isAlreadyComplete,
        String storeName,
        List<CheckoutItemSnapshot> itemSnapshots,
        Instant paymentProtectionExpiresAt
) {
    public PreparationResult(
            Payment payment,
            PaymentAttempt attempt,
            boolean isAlreadyComplete,
            String storeName,
            List<CheckoutItemSnapshot> itemSnapshots
    ) {
        this(payment, attempt, isAlreadyComplete, storeName, itemSnapshots, null);
    }
}
