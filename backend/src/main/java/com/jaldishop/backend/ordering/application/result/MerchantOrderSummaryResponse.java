package com.jaldishop.backend.ordering.application.result;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.model.OrderItem;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record MerchantOrderSummaryResponse(
        UUID id,
        String orderNumber,
        UUID storeId,
        UUID customerUserId,
        OrderStatus status,
        DeliveryMode deliveryMode,
        LocalDate serviceDate,
        LocalTime serviceStartTime,
        LocalTime serviceEndTime,
        String customerName,
        String customerPhone,
        String deliveryAddress,
        String deliveryReference,
        BigDecimal totalAmount,
        String currency,
        int totalItems,
        Instant confirmedAt,
        Instant updatedAt,
        List<MerchantOrderItemResponse> items
) {
    public static MerchantOrderSummaryResponse from(Order order) {
        if (order == null) {
            return null;
        }

        List<MerchantOrderItemResponse> items = order.getItems() != null
                ? order.getItems().stream()
                .map(item -> new MerchantOrderItemResponse(
                        item.id(),
                        item.variantId(),
                        item.productName(),
                        item.variantName(),
                        item.quantity(),
                        item.unitPriceAmount(),
                        item.subtotalAmount()
                ))
                .toList()
                : Collections.emptyList();

        int totalItems = order.getItems() != null
                ? order.getItems().stream().mapToInt(OrderItem::quantity).sum()
                : 0;

        return new MerchantOrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStoreId(),
                order.getUserId(),
                order.getStatus(),
                order.getDeliveryMode(),
                order.getServiceDate(),
                order.getServiceStartTime(),
                order.getServiceEndTime(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getDeliveryAddress(),
                order.getDeliveryReference(),
                order.getTotalAmount(),
                order.getCurrency(),
                totalItems,
                order.getConfirmedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
