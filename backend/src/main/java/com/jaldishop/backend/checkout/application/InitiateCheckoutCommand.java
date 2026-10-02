package com.jaldishop.backend.checkout.application;

import com.jaldishop.backend.checkout.domain.CheckoutFulfillmentType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record InitiateCheckoutCommand(
        UUID userId,
        UUID storeId,
        CheckoutFulfillmentType fulfillmentType,
        LocalDate serviceDate,
        LocalTime startTime,
        LocalTime endTime,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String discountCode
) {}
