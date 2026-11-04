package com.jaldishop.backend.payment.infrastructure.persistence.mapper;

import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentEntity;
import com.jaldishop.backend.shared.domain.model.CustomerSnapshot;
import com.jaldishop.backend.shared.domain.model.DeliveryAddressSnapshot;
import com.jaldishop.backend.shared.domain.model.MonetarySummary;
import org.springframework.stereotype.Component;

@Component
public class PaymentPersistenceMapper {

    public Payment toDomain(PaymentEntity entity) {
        if (entity == null) {
            return null;
        }

        FulfillmentType fulfillmentType = null;
        if (entity.getFulfillmentType() != null && !entity.getFulfillmentType().isBlank()) {
            fulfillmentType = FulfillmentType.valueOf(entity.getFulfillmentType().trim());
        }

        CustomerSnapshot customer = CustomerSnapshot.of(
                entity.getCustomerName(), entity.getCustomerPhone(), entity.getCustomerEmail()
        );
        DeliveryAddressSnapshot deliveryAddressSnapshot = DeliveryAddressSnapshot.of(
                entity.getDeliveryAddress(), entity.getDeliveryReference(),
                entity.getDeliveryLatitude(), entity.getDeliveryLongitude()
        );
        MonetarySummary pricing = MonetarySummary.of(
                entity.getProductsSubtotalAmount(),
                entity.getDiscountAmount(),
                entity.getDiscountCode(),
                entity.getDeliveryFeeAmount(),
                entity.getIncludedTaxAmount(),
                entity.getAmount(),
                entity.getCurrency()
        );

        return Payment.rehydrate(
                entity.getId(),
                entity.getCapacityReservationId(),
                pricing,
                PaymentStatus.valueOf(entity.getStatus()),
                RefundStatus.valueOf(entity.getRefundStatus()),
                entity.getRefundAmount(),
                entity.getRefundReference(),
                entity.getApprovedAt(),
                entity.getRefundedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                fulfillmentType,
                customer,
                deliveryAddressSnapshot,
                entity.getItemsSnapshot()
        );
    }

    public PaymentEntity toEntity(Payment domain) {
        if (domain == null) {
            return null;
        }

        String fulfillmentType = domain.getFulfillmentType() != null ? domain.getFulfillmentType().name() : null;

        return new PaymentEntity(
                domain.getId(),
                domain.getCapacityReservationId(),
                domain.getAmount(),
                domain.getCurrency(),
                domain.getStatus().name(),
                domain.getRefundStatus().name(),
                domain.getRefundAmount(),
                domain.getRefundReference(),
                domain.getApprovedAt(),
                domain.getRefundedAt(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                fulfillmentType,
                domain.getCustomerName(),
                domain.getCustomerPhone(),
                domain.getCustomerEmail(),
                domain.getDeliveryAddress(),
                domain.getDeliveryReference(),
                domain.getDeliveryLatitude(),
                domain.getDeliveryLongitude(),
                domain.getProductsSubtotalAmount(),
                domain.getDiscountAmount(),
                domain.getDiscountCode(),
                domain.getDeliveryFeeAmount(),
                domain.getIncludedTaxAmount(),
                domain.getItemsSnapshot()
        );
    }

}
