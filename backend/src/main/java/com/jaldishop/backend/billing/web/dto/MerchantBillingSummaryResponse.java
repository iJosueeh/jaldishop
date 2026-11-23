package com.jaldishop.backend.billing.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MerchantBillingSummaryResponse(
        UUID storeId,
        BigDecimal availableBalance,
        BigDecimal lockedBalance,
        BigDecimal totalPaidOut,
        BigDecimal minPayoutThreshold,
        String currency,
        MerchantPayoutDestinationResponse payoutDestination,
        Instant asOf
) {}
