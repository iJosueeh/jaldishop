package com.jaldishop.backend.payment.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "capacity_reservation_id", nullable = false)
    private UUID capacityReservationId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "refund_status", nullable = false, length = 30)
    private String refundStatus;

    @Column(name = "refund_amount", precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "refund_reference", length = 100)
    private String refundReference;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "fulfillment_type", length = 20)
    private String fulfillmentType;

    @Column(name = "customer_name", length = 200)
    private String customerName;

    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    @Column(name = "customer_email", length = 254)
    private String customerEmail;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_reference")
    private String deliveryReference;

    @Column(name = "delivery_latitude", precision = 9, scale = 6)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 9, scale = 6)
    private BigDecimal deliveryLongitude;

    @Column(name = "products_subtotal_amount", precision = 12, scale = 2)
    private BigDecimal productsSubtotalAmount;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "discount_code", length = 80)
    private String discountCode;

    @Column(name = "delivery_fee_amount", precision = 12, scale = 2)
    private BigDecimal deliveryFeeAmount;

    @Column(name = "included_tax_amount", precision = 12, scale = 2)
    private BigDecimal includedTaxAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_snapshot")
    private String itemsSnapshot;

    protected PaymentEntity() {
    }

    public PaymentEntity(
            UUID id,
            UUID capacityReservationId,
            BigDecimal amount,
            String currency,
            String status,
            String refundStatus,
            BigDecimal refundAmount,
            String refundReference,
            Instant approvedAt,
            Instant refundedAt,
            Instant createdAt,
            Instant updatedAt,
            String fulfillmentType,
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
        this.id = id;
        this.capacityReservationId = capacityReservationId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.refundStatus = refundStatus;
        this.refundAmount = refundAmount;
        this.refundReference = refundReference;
        this.approvedAt = approvedAt;
        this.refundedAt = refundedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fulfillmentType = fulfillmentType;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.deliveryAddress = deliveryAddress;
        this.deliveryReference = deliveryReference;
        this.deliveryLatitude = deliveryLatitude;
        this.deliveryLongitude = deliveryLongitude;
        this.productsSubtotalAmount = productsSubtotalAmount;
        this.discountAmount = discountAmount;
        this.discountCode = discountCode;
        this.deliveryFeeAmount = deliveryFeeAmount;
        this.includedTaxAmount = includedTaxAmount;
        this.itemsSnapshot = itemsSnapshot;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCapacityReservationId() { return capacityReservationId; }
    public void setCapacityReservationId(UUID capacityReservationId) { this.capacityReservationId = capacityReservationId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String refundStatus) { this.refundStatus = refundStatus; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
    public String getRefundReference() { return refundReference; }
    public void setRefundReference(String refundReference) { this.refundReference = refundReference; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Instant refundedAt) { this.refundedAt = refundedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getFulfillmentType() { return fulfillmentType; }
    public void setFulfillmentType(String fulfillmentType) { this.fulfillmentType = fulfillmentType; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getDeliveryReference() { return deliveryReference; }
    public void setDeliveryReference(String deliveryReference) { this.deliveryReference = deliveryReference; }
    public BigDecimal getDeliveryLatitude() { return deliveryLatitude; }
    public void setDeliveryLatitude(BigDecimal deliveryLatitude) { this.deliveryLatitude = deliveryLatitude; }
    public BigDecimal getDeliveryLongitude() { return deliveryLongitude; }
    public void setDeliveryLongitude(BigDecimal deliveryLongitude) { this.deliveryLongitude = deliveryLongitude; }

    public BigDecimal getProductsSubtotalAmount() { return productsSubtotalAmount; }
    public void setProductsSubtotalAmount(BigDecimal productsSubtotalAmount) { this.productsSubtotalAmount = productsSubtotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }
    public BigDecimal getDeliveryFeeAmount() { return deliveryFeeAmount; }
    public void setDeliveryFeeAmount(BigDecimal deliveryFeeAmount) { this.deliveryFeeAmount = deliveryFeeAmount; }
    public BigDecimal getIncludedTaxAmount() { return includedTaxAmount; }
    public void setIncludedTaxAmount(BigDecimal includedTaxAmount) { this.includedTaxAmount = includedTaxAmount; }

    public String getItemsSnapshot() { return itemsSnapshot; }
    public void setItemsSnapshot(String itemsSnapshot) { this.itemsSnapshot = itemsSnapshot; }

}
