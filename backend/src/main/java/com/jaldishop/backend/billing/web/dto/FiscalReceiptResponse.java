package com.jaldishop.backend.billing.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FiscalReceiptResponse(
        UUID id,
        UUID orderId,
        UUID paymentId,
        UUID storeId,
        String documentTypeCode,
        String documentTypeName,
        String series,
        long correlative,
        String fullNumber,
        String emitterTaxIdType,
        String emitterTaxIdNumber,
        String emitterLegalName,
        String emitterTaxRegime,
        String customerDocType,
        String customerDocNumber,
        String customerName,
        BigDecimal taxableAmount,
        BigDecimal igvRate,
        BigDecimal igvAmount,
        BigDecimal totalAmount,
        String currency,
        String paymentProvider,
        String providerPaymentId,
        Instant issuedAt
) {}
