package com.jaldishop.backend.checkout.web.dto;

import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CheckoutResponse(
        UUID reservationId,
        Instant reservationExpiresAt,
        UUID storeId,
        String storeName,
        UUID userId,
        CheckoutFulfillmentType fulfillmentType,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        CheckoutCustomerResponse customerInfo,
        List<CheckoutItemResponse> items,
        CheckoutPricingResponse pricing,
        Instant createdAt
) {}
