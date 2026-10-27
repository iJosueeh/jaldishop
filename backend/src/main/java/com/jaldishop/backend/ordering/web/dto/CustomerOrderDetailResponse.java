package com.jaldishop.backend.ordering.web.dto;

import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CustomerOrderDetailResponse(
        UUID id,
        String orderNumber,
        CustomerOrderStoreInfo store,
        OrderStatus status,
        DeliveryMode deliveryMode,
        LocalDate serviceDate,
        LocalTime serviceStartTime,
        LocalTime serviceEndTime,
        String customerName,
        String customerPhone,
        String customerEmail,
        String deliveryAddress,
        String deliveryReference,
        BigDecimal deliveryLatitude,
        BigDecimal deliveryLongitude,
        String currency,
        BigDecimal productsSubtotalAmount,
        BigDecimal discountAmount,
        String discountCode,
        BigDecimal deliveryFeeAmount,
        BigDecimal includedTaxAmount,
        BigDecimal totalAmount,
        Instant confirmedAt,
        Instant updatedAt,
        List<CustomerOrderItemResponse> items,
        List<CustomerOrderStatusHistoryResponse> statusHistory
) {}
