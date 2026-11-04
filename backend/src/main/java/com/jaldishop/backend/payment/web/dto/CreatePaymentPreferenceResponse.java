package com.jaldishop.backend.payment.web.dto;

import java.util.UUID;

public record CreatePaymentPreferenceResponse(
        UUID paymentId,
        String preferenceId,
        String initPoint,
        String status
) {}
