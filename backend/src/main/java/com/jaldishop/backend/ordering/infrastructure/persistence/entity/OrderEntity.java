package com.jaldishop.backend.ordering.infrastructure.persistence.entity;

import com.jaldishop.backend.ordering.domain.DeliveryMode;
import com.jaldishop.backend.ordering.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Column(name = "capacity_reservation_id", nullable = false)
    private UUID capacityReservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 30)
    private DeliveryMode deliveryMode;

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "service_start_time")
    private LocalTime serviceStartTime;

    @Column(name = "service_end_time")
    private LocalTime serviceEndTime;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 30)
    private String customerPhone;

    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;

    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "delivery_reference")
    private String deliveryReference;

    @Column(name = "delivery_latitude", precision = 9, scale = 6)
    private BigDecimal deliveryLatitude;

    @Column(name = "delivery_longitude", precision = 9, scale = 6)
    private BigDecimal deliveryLongitude;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "products_subtotal_amount", nullable = false)
    private BigDecimal productsSubtotalAmount;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "discount_code", length = 80)
    private String discountCode;

    @Column(name = "delivery_fee_amount", nullable = false)
    private BigDecimal deliveryFeeAmount;

    @Column(name = "included_tax_amount", nullable = false)
    private BigDecimal includedTaxAmount;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "confirmed_at", nullable = false)
    private Instant confirmedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItemEntity> items = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderStatusHistoryEntity> statusHistory = new ArrayList<>();

    protected OrderEntity() {
    }

    public OrderEntity(UUID id, String orderNumber, UUID userId, UUID storeId, UUID paymentId,
                       UUID capacityReservationId, OrderStatus status, DeliveryMode deliveryMode,
                       LocalDate serviceDate, LocalTime serviceStartTime, LocalTime serviceEndTime,
                       String customerName, String customerPhone, String customerEmail,
                       String deliveryAddress, String deliveryReference,
                       BigDecimal deliveryLatitude, BigDecimal deliveryLongitude,
                       String currency, BigDecimal productsSubtotalAmount, BigDecimal discountAmount,
                       String discountCode, BigDecimal deliveryFeeAmount, BigDecimal includedTaxAmount,
                       BigDecimal totalAmount, Instant confirmedAt, Instant updatedAt) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.storeId = storeId;
        this.paymentId = paymentId;
        this.capacityReservationId = capacityReservationId;
        this.status = status;
        this.deliveryMode = deliveryMode;
        this.serviceDate = serviceDate;
        this.serviceStartTime = serviceStartTime;
        this.serviceEndTime = serviceEndTime;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.customerEmail = customerEmail;
        this.deliveryAddress = deliveryAddress;
        this.deliveryReference = deliveryReference;
        this.deliveryLatitude = deliveryLatitude;
        this.deliveryLongitude = deliveryLongitude;
        this.currency = currency;
        this.productsSubtotalAmount = productsSubtotalAmount;
        this.discountAmount = discountAmount;
        this.discountCode = discountCode;
        this.deliveryFeeAmount = deliveryFeeAmount;
        this.includedTaxAmount = includedTaxAmount;
        this.totalAmount = totalAmount;
        this.confirmedAt = confirmedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getStoreId() { return storeId; }
    public void setStoreId(UUID storeId) { this.storeId = storeId; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getCapacityReservationId() { return capacityReservationId; }
    public void setCapacityReservationId(UUID capacityReservationId) { this.capacityReservationId = capacityReservationId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public void setDeliveryMode(DeliveryMode deliveryMode) { this.deliveryMode = deliveryMode; }
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public LocalTime getServiceStartTime() { return serviceStartTime; }
    public void setServiceStartTime(LocalTime serviceStartTime) { this.serviceStartTime = serviceStartTime; }
    public LocalTime getServiceEndTime() { return serviceEndTime; }
    public void setServiceEndTime(LocalTime serviceEndTime) { this.serviceEndTime = serviceEndTime; }
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
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
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
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<OrderItemEntity> getItems() { return items; }
    public void setItems(List<OrderItemEntity> items) { this.items = items != null ? items : new ArrayList<>(); }
    public List<OrderStatusHistoryEntity> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<OrderStatusHistoryEntity> statusHistory) { this.statusHistory = statusHistory != null ? statusHistory : new ArrayList<>(); }

}