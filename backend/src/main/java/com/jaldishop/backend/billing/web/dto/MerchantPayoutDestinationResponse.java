package com.jaldishop.backend.billing.web.dto;

import java.time.Instant;
import java.util.UUID;

public record MerchantPayoutDestinationResponse(
        UUID id,
        UUID storeId,
        String payoutChannel,
        String bankName,
        String accountNumber,
        String accountCci,
        String phoneNumber,
        String accountHolderName,
        String accountHolderDocType,
        String accountHolderDocNumber,
        boolean isPrimary,
        boolean isVerified,
        Instant updatedAt
) {}
