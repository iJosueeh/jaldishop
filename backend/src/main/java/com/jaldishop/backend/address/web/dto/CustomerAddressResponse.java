package com.jaldishop.backend.address.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CustomerAddressResponse(
        UUID id,
        String alias,
        String recipientName,
        String recipientPhone,
        String streetAddress,
        String department,
        String province,
        String district,
        String ubigeoCode,
        String reference,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt
) {}
