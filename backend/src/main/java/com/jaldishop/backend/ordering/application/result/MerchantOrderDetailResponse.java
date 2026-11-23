package com.jaldishop.backend.ordering.application.result;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record MerchantOrderDetailResponse(
        UUID id,
        String orderNumber,
        UUID storeId,
        UUID customerUserId,
        UUID paymentId,
        UUID capacityReservationId,
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
        OrderPaymentBreakdownResponse paymentBreakdown,
        Instant confirmedAt,
        Instant updatedAt,
        List<MerchantOrderItemResponse> items,
        List<MerchantOrderStatusHistoryResponse> statusHistory
) {
    public MerchantOrderDetailResponse(
            UUID id,
            String orderNumber,
            UUID storeId,
            UUID customerUserId,
            UUID paymentId,
            UUID capacityReservationId,
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
            List<MerchantOrderItemResponse> items,
            List<MerchantOrderStatusHistoryResponse> statusHistory
    ) {
        this(
                id, orderNumber, storeId, customerUserId, paymentId, capacityReservationId,
                status, deliveryMode, serviceDate, serviceStartTime, serviceEndTime,
                customerName, customerPhone, customerEmail, deliveryAddress, deliveryReference,
                deliveryLatitude, deliveryLongitude, currency, productsSubtotalAmount,
                discountAmount, discountCode, deliveryFeeAmount, includedTaxAmount, totalAmount,
                OrderPaymentBreakdownResponse.compute(productsSubtotalAmount, deliveryFeeAmount, new BigDecimal("0.0350"), new BigDecimal("0.90")),
                confirmedAt, updatedAt, items, statusHistory
        );
    }

    public static MerchantOrderDetailResponse from(Order order) {
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

        List<MerchantOrderStatusHistoryResponse> statusHistory = order.getStatusHistory() != null
                ? order.getStatusHistory().stream()
                .map(history -> new MerchantOrderStatusHistoryResponse(
                        history.id(),
                        history.changedByUserId(),
                        history.status(),
                        history.reason(),
                        history.changedAt()
                ))
                .toList()
                : Collections.emptyList();

        OrderPaymentBreakdownResponse breakdown = OrderPaymentBreakdownResponse.compute(
                order.getProductsSubtotalAmount(),
                order.getDeliveryFeeAmount(),
                new BigDecimal("0.0350"),
                new BigDecimal("0.90")
        );

        return new MerchantOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStoreId(),
                order.getUserId(),
                order.getPaymentId(),
                order.getCapacityReservationId(),
                order.getStatus(),
                order.getDeliveryMode(),
                order.getServiceDate(),
                order.getServiceStartTime(),
                order.getServiceEndTime(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                order.getCustomerEmail(),
                order.getDeliveryAddress(),
                order.getDeliveryReference(),
                order.getDeliveryLatitude(),
                order.getDeliveryLongitude(),
                order.getCurrency(),
                order.getProductsSubtotalAmount(),
                order.getDiscountAmount(),
                order.getDiscountCode(),
                order.getDeliveryFeeAmount(),
                order.getIncludedTaxAmount(),
                order.getTotalAmount(),
                breakdown,
                order.getConfirmedAt(),
                order.getUpdatedAt(),
                items,
                statusHistory
        );
    }
}
