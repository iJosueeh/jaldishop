package com.jaldishop.backend.ordering.domain.repository;

import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.model.OrderStatusCounts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(UUID id);
    Optional<Order> findByIdForUpdate(UUID id);
    Optional<Order> findByPaymentId(UUID paymentId);
    List<Order> findByPaymentIdIn(List<UUID> paymentIds);
    Optional<Order> findByCapacityReservationId(UUID capacityReservationId);
    List<Order> findByUserId(UUID userId);
    Page<Order> findPagedByUserId(UUID userId, boolean activeOnly, DeliveryMode deliveryMode, String searchPattern, Pageable pageable);
    Optional<Order> findByOrderNumber(String orderNumber);
    long nextOrderNumber();
    boolean hasCompletedOrderWithProduct(UUID userId, UUID productId);
    boolean hasInProgressOrderWithProduct(UUID userId, UUID productId);

    Page<Order> findByStoreId(UUID storeId, Pageable pageable);
    Page<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status, Pageable pageable);
    Page<Order> searchOrders(UUID storeId, OrderStatus status, String searchPattern, Pageable pageable);
    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);
    Optional<Order> findByOrderNumberAndStoreId(String orderNumber, UUID storeId);

    OrderStatusCounts countByStoreIdAndServiceDate(UUID storeId, LocalDate serviceDate);
}
