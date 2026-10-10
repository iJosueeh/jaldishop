package com.jaldishop.backend.ordering.infrastructure.persistence.mapper;

import com.jaldishop.backend.ordering.domain.Order;
import com.jaldishop.backend.ordering.domain.OrderItem;
import com.jaldishop.backend.ordering.domain.OrderStatusHistory;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderItemEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderPersistenceMapper {

    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }
        return Order.reconstitute(
                entity.getId(), entity.getOrderNumber(), entity.getUserId(), entity.getStoreId(),
                entity.getPaymentId(), entity.getCapacityReservationId(), entity.getDeliveryMode(),
                entity.getServiceDate(), entity.getServiceStartTime(), entity.getServiceEndTime(),
                entity.getCustomerName(), entity.getCustomerPhone(), entity.getCustomerEmail(),
                entity.getDeliveryAddress(), entity.getDeliveryReference(),
                entity.getDeliveryLatitude(), entity.getDeliveryLongitude(),
                entity.getCurrency(), entity.getProductsSubtotalAmount(), entity.getDiscountAmount(),
                entity.getDiscountCode(), entity.getDeliveryFeeAmount(), entity.getIncludedTaxAmount(),
                entity.getTotalAmount(), entity.getConfirmedAt(), entity.getStatus(), entity.getUpdatedAt(),
                entity.getItems().stream().map(this::toDomainItem).toList(),
                entity.getStatusHistory().stream().map(this::toDomainHistory).toList()
        );
    }

    public OrderEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }
        OrderEntity entity = new OrderEntity(
                domain.getId(), domain.getOrderNumber(), domain.getUserId(), domain.getStoreId(),
                domain.getPaymentId(), domain.getCapacityReservationId(), domain.getStatus(),
                domain.getDeliveryMode(), domain.getServiceDate(), domain.getServiceStartTime(),
                domain.getServiceEndTime(), domain.getCustomerName(), domain.getCustomerPhone(),
                domain.getCustomerEmail(), domain.getDeliveryAddress(), domain.getDeliveryReference(),
                domain.getDeliveryLatitude(), domain.getDeliveryLongitude(),
                domain.getCurrency(), domain.getProductsSubtotalAmount(), domain.getDiscountAmount(),
                domain.getDiscountCode(), domain.getDeliveryFeeAmount(), domain.getIncludedTaxAmount(),
                domain.getTotalAmount(), domain.getConfirmedAt(), domain.getUpdatedAt()
        );

        entity.setItems(domain.getItems().stream()
                .map(item -> toEntityItem(item, domain.getStoreId()))
                .toList());
        entity.setStatusHistory(domain.getStatusHistory().stream()
                .map(this::toEntityHistory)
                .toList());
        return entity;
    }

    private OrderItem toDomainItem(OrderItemEntity entity) {
        return new OrderItem(
                entity.getId(), entity.getOrderId(), entity.getVariantId(),
                entity.getProductName(), entity.getVariantName(), entity.getQuantity(),
                entity.getUnitPriceAmount(), entity.getSubtotalAmount()
        );
    }

    private OrderItemEntity toEntityItem(OrderItem domain, UUID storeId) {
        return new OrderItemEntity(
                domain.id(), domain.orderId(), storeId, domain.variantId(),
                domain.productName(), domain.variantName(), domain.quantity(),
                domain.unitPriceAmount(), domain.subtotalAmount()
        );
    }

    private OrderStatusHistory toDomainHistory(OrderStatusHistoryEntity entity) {
        return new OrderStatusHistory(
                entity.getId(), entity.getOrderId(), entity.getChangedByUserId(),
                entity.getStatus(), entity.getReason(), entity.getChangedAt()
        );
    }

    private OrderStatusHistoryEntity toEntityHistory(OrderStatusHistory domain) {
        return new OrderStatusHistoryEntity(
                domain.id(), domain.orderId(), domain.changedByUserId(),
                domain.status(), domain.reason(), domain.changedAt()
        );
    }

}