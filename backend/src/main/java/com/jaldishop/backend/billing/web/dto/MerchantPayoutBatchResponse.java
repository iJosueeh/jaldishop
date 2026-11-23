package com.jaldishop.backend.billing.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MerchantPayoutBatchResponse(
        UUID id,
        UUID storeId,
        String status,
        BigDecimal grossAmount,
        BigDecimal bankingFee,
        BigDecimal netDisbursedAmount,
        String currency,
        String bankOperationNumber,
        LocalDate scheduledFor,
        Instant processedAt,
        Instant createdAt
) {}
