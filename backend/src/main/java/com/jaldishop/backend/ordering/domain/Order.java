package com.jaldishop.backend.ordering.domain;

import com.jaldishop.backend.shared.exception.BusinessRuleException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {

    private final UUID id;
    private final String orderNumber;
    private final UUID userId;
    private final UUID storeId;
    private final UUID paymentId;
    private final UUID capacityReservationId;
    private final DeliveryMode deliveryMode;
    private final LocalDate serviceDate;
    private final LocalTime serviceStartTime;
    private final LocalTime serviceEndTime;
    private final String customerName;
    private final String customerPhone;
    private final String customerEmail;
    private final String deliveryAddress;
    private final String deliveryReference;
    private final BigDecimal deliveryLatitude;
    private final BigDecimal deliveryLongitude;
    private final String currency;
    private final BigDecimal productsSubtotalAmount;
    private final BigDecimal discountAmount;
    private final String discountCode;
    private final BigDecimal deliveryFeeAmount;
    private final BigDecimal includedTaxAmount;
    private final BigDecimal totalAmount;
    private final Instant confirmedAt;
    private OrderStatus status;
    private Instant updatedAt;
    private final List<OrderItem> items;
    private final List<OrderStatusHistory> statusHistory;

    private Order(UUID id, String orderNumber, UUID userId, UUID storeId, UUID paymentId,
                  UUID capacityReservationId, DeliveryMode deliveryMode, LocalDate serviceDate,
                  LocalTime serviceStartTime, LocalTime serviceEndTime, String customerName,
                  String customerPhone, String customerEmail, String deliveryAddress,
                  String deliveryReference, BigDecimal deliveryLatitude, BigDecimal deliveryLongitude,
                  String currency, BigDecimal productsSubtotalAmount, BigDecimal discountAmount,
                  String discountCode, BigDecimal deliveryFeeAmount, BigDecimal includedTaxAmount,
                  BigDecimal totalAmount, Instant confirmedAt, OrderStatus status, Instant updatedAt,
                  List<OrderItem> items, List<OrderStatusHistory> statusHistory) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.userId = userId;
        this.storeId = storeId;
        this.paymentId = paymentId;
        this.capacityReservationId = capacityReservationId;
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
        this.status = status;
        this.updatedAt = updatedAt;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.statusHistory = statusHistory != null ? new ArrayList<>(statusHistory) : new ArrayList<>();
    }

    public static Order reconstitute(UUID id, String orderNumber, UUID userId, UUID storeId, UUID paymentId,
                                     UUID capacityReservationId, DeliveryMode deliveryMode, LocalDate serviceDate,
                                     LocalTime serviceStartTime, LocalTime serviceEndTime, String customerName,
                                     String customerPhone, String customerEmail, String deliveryAddress,
                                     String deliveryReference, BigDecimal deliveryLatitude, BigDecimal deliveryLongitude,
                                     String currency, BigDecimal productsSubtotalAmount, BigDecimal discountAmount,
                                     String discountCode, BigDecimal deliveryFeeAmount, BigDecimal includedTaxAmount,
                                     BigDecimal totalAmount, Instant confirmedAt, OrderStatus status, Instant updatedAt,
                                     List<OrderItem> items, List<OrderStatusHistory> statusHistory) {
        return new Order(
                id, orderNumber, userId, storeId, paymentId, capacityReservationId, deliveryMode, serviceDate,
                serviceStartTime, serviceEndTime, customerName, customerPhone, customerEmail, deliveryAddress,
                deliveryReference, deliveryLatitude, deliveryLongitude, currency, productsSubtotalAmount, discountAmount,
                discountCode, deliveryFeeAmount, includedTaxAmount, totalAmount, confirmedAt, status, updatedAt,
                items, statusHistory
        );
    }

    public static Order createConfirmed(CreateData data, String orderNumber) {
        if (data == null) {
            throw new IllegalArgumentException("Los datos de la orden son obligatorios.");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new BusinessRuleException("RN-ORD-02", "El número de orden es obligatorio.");
        }
        if (data.userId() == null) {
            throw new BusinessRuleException("RN-ORD-03", "El usuario es obligatorio para la orden.");
        }
        if (data.storeId() == null) {
            throw new BusinessRuleException("RN-ORD-04", "La tienda es obligatoria para la orden.");
        }
        if (data.paymentId() == null) {
            throw new BusinessRuleException("RN-PAY-02", "Una orden solo puede crearse con un pago asociado.");
        }
        if (data.capacityReservationId() == null) {
            throw new BusinessRuleException("RN-CAP-01", "Una orden requiere una reserva de capacidad asociada.");
        }
        if (data.deliveryMode() == null) {
            throw new BusinessRuleException("RN-DEL-01", "La modalidad de entrega es obligatoria.");
        }
        if (data.deliveryMode() == DeliveryMode.DELIVERY
                && (data.deliveryAddress() == null || data.deliveryAddress().isBlank())) {
            throw new BusinessRuleException("RN-DEL-05", "La dirección de entrega es obligatoria para modalidad DELIVERY.");
        }
        if (data.items() == null || data.items().isEmpty()) {
            throw new BusinessRuleException("RN-ORD-06", "Una orden debe contener al menos un ítem.");
        }
        validateServiceWindow(data.serviceStartTime(), data.serviceEndTime());
        validateAmounts(data.productsSubtotalAmount(), data.discountAmount(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount());
        if (data.currency() == null || data.currency().isBlank()) {
            throw new BusinessRuleException("RN-ORD-08", "La moneda de la orden es obligatoria.");
        }

        Instant now = Instant.now();
        Order order = new Order(
                UUID.randomUUID(), orderNumber, data.userId(), data.storeId(), data.paymentId(),
                data.capacityReservationId(), data.deliveryMode(), data.serviceDate(), data.serviceStartTime(),
                data.serviceEndTime(), data.customerName(), data.customerPhone(), data.customerEmail(),
                data.deliveryAddress(), data.deliveryReference(), data.deliveryLatitude(), data.deliveryLongitude(),
                data.currency(), data.productsSubtotalAmount(), data.discountAmount(), data.discountCode(),
                data.deliveryFeeAmount(), data.includedTaxAmount(), data.totalAmount(), now,
                OrderStatus.CONFIRMED, now, new ArrayList<>(), new ArrayList<>()
        );

        for (ItemData item : data.items()) {
            order.items.add(OrderItem.create(order.id, item));
        }
        order.statusHistory.add(OrderStatusHistory.create(
                order.id, null, OrderStatus.CONFIRMED, "Pedido confirmado por pago aprobado.", now
        ));

        return order;
    }

    public void transitionTo(OrderStatus newStatus, UUID changedByUserId, String reason) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new BusinessRuleException(
                    "RN-ORD-07",
                    String.format("Transición inválida de estado: no se puede pasar de %s a %s", this.status, newStatus)
            );
        }
        if (newStatus == OrderStatus.OUT_FOR_DELIVERY && this.deliveryMode != DeliveryMode.DELIVERY) {
            throw new BusinessRuleException(
                    "RN-DEL-04",
                    "El estado OUT_FOR_DELIVERY solo está permitido para pedidos con modalidad DELIVERY"
            );
        }
        Instant now = Instant.now();
        this.status = newStatus;
        this.updatedAt = now;
        this.statusHistory.add(OrderStatusHistory.create(this.id, changedByUserId, newStatus, reason, now));
    }

    private static void validateServiceWindow(LocalTime startTime, LocalTime endTime) {
        if (startTime == null && endTime == null) {
            return;
        }
        if (startTime == null || endTime == null) {
            throw new BusinessRuleException("RN-ORD-05", "La franja de servicio debe indicar inicio y fin juntos.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessRuleException("RN-ORD-05", "El inicio de la franja de servicio debe ser anterior al fin.");
        }
    }

    private static void validateAmounts(BigDecimal subtotal, BigDecimal discount, BigDecimal deliveryFee,
                                        BigDecimal includedTax, BigDecimal total) {
        if (subtotal == null || subtotal.signum() < 0
                || discount == null || discount.signum() < 0
                || deliveryFee == null || deliveryFee.signum() < 0
                || includedTax == null || includedTax.signum() < 0
                || total == null || total.signum() < 0) {
            throw new BusinessRuleException("RN-ORD-09", "Los montos de la orden no pueden ser negativos.");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new BusinessRuleException("RN-ORD-10", "El descuento no puede superar el subtotal de productos.");
        }
    }

    public UUID getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public UUID getUserId() { return userId; }
    public UUID getStoreId() { return storeId; }
    public UUID getPaymentId() { return paymentId; }
    public UUID getCapacityReservationId() { return capacityReservationId; }
    public DeliveryMode getDeliveryMode() { return deliveryMode; }
    public LocalDate getServiceDate() { return serviceDate; }
    public LocalTime getServiceStartTime() { return serviceStartTime; }
    public LocalTime getServiceEndTime() { return serviceEndTime; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public String getCustomerEmail() { return customerEmail; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getDeliveryReference() { return deliveryReference; }
    public BigDecimal getDeliveryLatitude() { return deliveryLatitude; }
    public BigDecimal getDeliveryLongitude() { return deliveryLongitude; }
    public String getCurrency() { return currency; }
    public BigDecimal getProductsSubtotalAmount() { return productsSubtotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getDiscountCode() { return discountCode; }
    public BigDecimal getDeliveryFeeAmount() { return deliveryFeeAmount; }
    public BigDecimal getIncludedTaxAmount() { return includedTaxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public OrderStatus getStatus() { return status; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public List<OrderStatusHistory> getStatusHistory() { return Collections.unmodifiableList(statusHistory); }

    public record CreateData(
            UUID userId,
            UUID storeId,
            UUID paymentId,
            UUID capacityReservationId,
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
            List<ItemData> items
    ) {}

    public record ItemData(
            UUID variantId,
            String productName,
            String variantName,
            int quantity,
            BigDecimal unitPriceAmount
    ) {}

}