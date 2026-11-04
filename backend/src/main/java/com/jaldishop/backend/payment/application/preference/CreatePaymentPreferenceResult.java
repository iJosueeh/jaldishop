package com.jaldishop.backend.payment.application.preference;

import java.util.Objects;
import java.util.UUID;

public record CreatePaymentPreferenceResult(
        UUID paymentId,
        String preferenceId,
        String initPoint,
        String status
) {
    public CreatePaymentPreferenceResult {
        Objects.requireNonNull(paymentId, "paymentId no puede ser nulo");
        Objects.requireNonNull(preferenceId, "preferenceId no puede ser nulo");
        Objects.requireNonNull(initPoint, "initPoint no puede ser nulo");
        Objects.requireNonNull(status, "status no puede ser nulo");
    }
}
