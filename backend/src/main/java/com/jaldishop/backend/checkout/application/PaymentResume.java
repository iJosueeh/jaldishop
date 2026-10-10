package com.jaldishop.backend.checkout.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResume(
        UUID paymentId,
        UUID capacityReservationId,
        String status,
        Instant approvedAt,
        BigDecimal amount,
        String currency
) {}