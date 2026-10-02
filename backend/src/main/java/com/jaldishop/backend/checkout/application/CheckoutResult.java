package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;
import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.checkout.domain.CheckoutPricing;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CheckoutResult(
        UUID reservationId,
        Instant reservationExpiresAt,
        UUID storeId,
        String storeName,
        UUID userId,
        CheckoutFulfillmentType fulfillmentType,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference,
        List<CheckoutItemSnapshot> items,
        CheckoutPricing pricing,
        Instant createdAt
) {}
