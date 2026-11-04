package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.shared.domain.model.CustomerSnapshot;
import com.jaldishop.backend.shared.domain.model.DeliveryAddressSnapshot;
import com.jaldishop.backend.shared.domain.model.MonetarySummary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Payment {

    private final UUID id;
    private final UUID capacityReservationId;
    private final MonetarySummary pricing;
    private PaymentStatus status;
    private RefundStatus refundStatus;
    private BigDecimal refundAmount;
    private String refundReference;
    private Instant approvedAt;
    private Instant refundedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    private final FulfillmentType fulfillmentType;
    private final CustomerSnapshot customer;
    private final DeliveryAddressSnapshot deliveryAddressSnapshot;

    private final String itemsSnapshot;

    public static Payment createPending(
            UUID capacityReservationId,
            MonetarySummary pricing,
            FulfillmentType fulfillmentType,
            CustomerSnapshot customer,
            DeliveryAddressSnapshot deliveryAddressSnapshot,
            String itemsSnapshot
    ) {
        Objects.requireNonNull(capacityReservationId, "capacityReservationId no puede ser nulo");
        Objects.requireNonNull(pricing, "pricing no puede ser nulo");
        if (pricing.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto del pago debe ser mayor a cero");
        }
        if (!"PEN".equals(pricing.currency())) {
            throw new IllegalArgumentException("La moneda permitida es estrictamente PEN");
        }

        Instant now = Instant.now();
        return new Payment(
                UUID.randomUUID(),
                capacityReservationId,
                pricing,
                PaymentStatus.PENDING,
                RefundStatus.NOT_REQUIRED,
                null,
                null,
                null,
                null,
                now,
                now,
                fulfillmentType,
                customer != null ? customer : CustomerSnapshot.of(null, null, null),
                deliveryAddressSnapshot != null ? deliveryAddressSnapshot : DeliveryAddressSnapshot.of(null, null, null, null),
                itemsSnapshot
        );
    }

    public static Payment createPending(
            UUID capacityReservationId,
            BigDecimal amount,
            String currency,
            FulfillmentType fulfillmentType,
            String customerName,
            String customerPhone,
            String customerEmail,
            String deliveryAddress,
            String deliveryReference,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude,
            BigDecimal productsSubtotalAmount,
            BigDecimal discountAmount,
            String discountCode,
            BigDecimal deliveryFeeAmount,
            BigDecimal includedTaxAmount,
            String itemsSnapshot
    ) {
        MonetarySummary pricing = MonetarySummary.of(
                productsSubtotalAmount,
                discountAmount != null ? discountAmount : BigDecimal.ZERO,
                discountCode,
                deliveryFeeAmount != null ? deliveryFeeAmount : BigDecimal.ZERO,
                includedTaxAmount != null ? includedTaxAmount : BigDecimal.ZERO,
                amount,
                currency
        );
        CustomerSnapshot customer = CustomerSnapshot.of(customerName, customerPhone, customerEmail);
        DeliveryAddressSnapshot addressSnapshot = DeliveryAddressSnapshot.of(
                deliveryAddress, deliveryReference, deliveryLatitude, deliveryLongitude
        );
        return createPending(
                capacityReservationId,
                pricing,
                fulfillmentType,
                customer,
                addressSnapshot,
                itemsSnapshot
        );
    }

    public static Payment rehydrate(
            UUID id,
            UUID capacityReservationId,
            MonetarySummary pricing,
            PaymentStatus status,
            RefundStatus refundStatus,
            BigDecimal refundAmount,
            String refundReference,
            Instant approvedAt,
            Instant refundedAt,
            Instant createdAt,
            Instant updatedAt,
            FulfillmentType fulfillmentType,
            CustomerSnapshot customer,
            DeliveryAddressSnapshot deliveryAddressSnapshot,
            String itemsSnapshot
    ) {
        return new Payment(
                id, capacityReservationId, pricing, status, refundStatus,
                refundAmount, refundReference, approvedAt, refundedAt, createdAt, updatedAt,
                fulfillmentType, customer, deliveryAddressSnapshot, itemsSnapshot
        );
    }

    public static Payment rehydrate(
            UUID id,
            UUID capacityReservationId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            RefundStatus refundStatus,
            BigDecimal refundAmount,
            String refundReference,
            Instant approvedAt,
            Instant refundedAt,
            Instant createdAt,
            Instant updatedAt,
            FulfillmentType fulfillmentType,
            String customerName,
            String customerPhone,
            String customerEmail,
            String deliveryAddress,
            String deliveryReference,
            BigDecimal deliveryLatitude,
            BigDecimal deliveryLongitude,
            BigDecimal productsSubtotalAmount,
            BigDecimal discountAmount,
            String discountCode,
            BigDecimal deliveryFeeAmount,
            BigDecimal includedTaxAmount,
            String itemsSnapshot
    ) {
        return rehydrate(
                id, capacityReservationId,
                MonetarySummary.of(productsSubtotalAmount, discountAmount, discountCode, deliveryFeeAmount, includedTaxAmount, amount, currency),
                status, refundStatus, refundAmount, refundReference, approvedAt, refundedAt, createdAt, updatedAt,
                fulfillmentType,
                CustomerSnapshot.of(customerName, customerPhone, customerEmail),
                DeliveryAddressSnapshot.of(deliveryAddress, deliveryReference, deliveryLatitude, deliveryLongitude),
                itemsSnapshot
        );
    }

    private Payment(
            UUID id,
            UUID capacityReservationId,
            MonetarySummary pricing,
            PaymentStatus status,
            RefundStatus refundStatus,
            BigDecimal refundAmount,
            String refundReference,
            Instant approvedAt,
            Instant refundedAt,
            Instant createdAt,
            Instant updatedAt,
            FulfillmentType fulfillmentType,
            CustomerSnapshot customer,
            DeliveryAddressSnapshot deliveryAddressSnapshot,
            String itemsSnapshot
    ) {
        this.id = id;
        this.capacityReservationId = capacityReservationId;
        this.pricing = pricing;
        this.status = status;
        this.refundStatus = refundStatus;
        this.refundAmount = refundAmount;
        this.refundReference = refundReference;
        this.approvedAt = approvedAt;
        this.refundedAt = refundedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fulfillmentType = fulfillmentType;
        this.customer = customer;
        this.deliveryAddressSnapshot = deliveryAddressSnapshot;
        this.itemsSnapshot = itemsSnapshot;
    }

    public void markApproved(Instant approvedAt) {
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = approvedAt != null ? approvedAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markProcessing() {
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public void requestRefund(BigDecimal amountToRefund) {
        requestRefund(amountToRefund, null);
    }

    public void requestRefund(BigDecimal amountToRefund, String refundReference) {
        if (amountToRefund == null || amountToRefund.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto a reembolsar debe ser mayor a cero");
        }
        if (amountToRefund.compareTo(getAmount()) > 0) {
            throw new IllegalArgumentException("El monto a reembolsar no puede superar el monto original pagado");
        }
        this.refundStatus = RefundStatus.PENDING;
        this.refundAmount = amountToRefund;
        this.refundReference = refundReference;
        this.updatedAt = Instant.now();
    }

    public void markRefundProcessing() {
        this.refundStatus = RefundStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void completeRefund(String refundReference, Instant refundedAt) {
        this.refundStatus = RefundStatus.COMPLETED;
        this.refundReference = refundReference;
        this.refundedAt = refundedAt != null ? refundedAt : Instant.now();
        this.updatedAt = Instant.now();
    }

    public void failRefund() {
        this.refundStatus = RefundStatus.FAILED;
        this.updatedAt = Instant.now();
    }

    public boolean isApproved() {
        return this.status == PaymentStatus.APPROVED;
    }

    public UUID getId() { return id; }
    public UUID getCapacityReservationId() { return capacityReservationId; }
    public MonetarySummary getPricing() { return pricing; }
    public BigDecimal getAmount() { return pricing != null ? pricing.total() : null; }
    public String getCurrency() { return pricing != null ? pricing.currency() : "PEN"; }
    public PaymentStatus getStatus() { return status; }
    public RefundStatus getRefundStatus() { return refundStatus; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public String getRefundReference() { return refundReference; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public FulfillmentType getFulfillmentType() { return fulfillmentType; }
    public CustomerSnapshot getCustomer() { return customer; }
    public String getCustomerName() { return customer != null ? customer.name() : null; }
    public String getCustomerPhone() { return customer != null ? customer.phone() : null; }
    public String getCustomerEmail() { return customer != null ? customer.email() : null; }

    public DeliveryAddressSnapshot getDeliveryAddressSnapshot() { return deliveryAddressSnapshot; }
    public String getDeliveryAddress() { return deliveryAddressSnapshot != null ? deliveryAddressSnapshot.address() : null; }
    public String getDeliveryReference() { return deliveryAddressSnapshot != null ? deliveryAddressSnapshot.reference() : null; }
    public BigDecimal getDeliveryLatitude() { return deliveryAddressSnapshot != null ? deliveryAddressSnapshot.latitude() : null; }
    public BigDecimal getDeliveryLongitude() { return deliveryAddressSnapshot != null ? deliveryAddressSnapshot.longitude() : null; }

    public BigDecimal getProductsSubtotalAmount() { return pricing != null ? pricing.subtotal() : null; }
    public BigDecimal getDiscountAmount() { return pricing != null ? pricing.discount() : null; }
    public String getDiscountCode() { return pricing != null ? pricing.discountCode() : null; }
    public BigDecimal getDeliveryFeeAmount() { return pricing != null ? pricing.deliveryFee() : null; }
    public BigDecimal getIncludedTaxAmount() { return pricing != null ? pricing.tax() : null; }
    public String getItemsSnapshot() { return itemsSnapshot; }

}
